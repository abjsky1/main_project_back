package main_project.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // 마지막으로 조회한 환율 JSON 저장
    private String latestJson;

    // 구독과 환율 갱신이 동시에 처리되지 않도록 synchronized 추가
    public synchronized SseEmitter subscribe() {

        SseEmitter emitter = new SseEmitter();

        emitter.onCompletion(() -> emitters.remove(emitter));

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete(); // [추가] 시간 초과 시 연결 종료
        });

        emitter.onError(error -> emitters.remove(emitter));

        emitters.add(emitter);

        // 새로 접속한 사용자에게 저장된 환율 즉시 전송
        if (latestJson != null) {
            send(emitter, latestJson);
        }

        return emitter;
    }

    // subscribe()와 같은 잠금을 사용하도록 synchronized 추가
    public synchronized void onMessage(String message) {

        // 접속한 사용자가 없어도 마지막 환율 저장
        latestJson = message;
        for (SseEmitter emitter : emitters) {
            // 공통 전송 메서드 호출
            send(emitter, message);
        }
    }

    // 기존 onMessage()의 전송 코드를 공통 메서드로 분리
    // 구독 직후 전송과 전체 사용자 전송에서 함께 사용
    private void send(SseEmitter emitter, String message) {
        try {emitter.send( SseEmitter.event()
                            .name("notice")
                            .data(message));
        } catch (IOException e) {
            emitters.remove(emitter);
        } catch (IllegalStateException e) {
            // [추가] 사용할 수 없는 연결 제거 및 오류 처리
            emitters.remove(emitter); emitter.completeWithError(e);}
    }
}
