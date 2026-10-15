
package main_project.matching.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MatchingSseService {

    // [1] 회원별 SSE 연결 정보 저장
    private final Map<String, List<SseEmitter>> emitters =
            new ConcurrentHashMap<>();


    // [2] 클라이언트 구독 처리
    public SseEmitter subscribe(String memberId) {

        // 2-1. SSE 연결 객체 생성
        SseEmitter emitter = new SseEmitter();

        // 2-2. 회원별 연결 정보 저장
        emitters.compute(memberId, (id, list) -> {

            // 처음 연결한 회원이면 리스트 생성
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
            }

            // 연결 정보 저장
            list.add(emitter);

            return list;
        });

        // 2-3. 연결이 정상 종료되면 삭제
        emitter.onCompletion(() -> remove(memberId, emitter));

        // 2-4. 연결 시간이 초과되면 삭제
        emitter.onTimeout(() -> remove(memberId, emitter));

        // 2-5. 연결 오류가 발생하면 삭제
        emitter.onError(e -> remove(memberId, emitter));

        // 2-6. 최초 연결 확인 메시지
        try {
            emitter.send(SseEmitter.event().name("connected").data("연결 성공"));

        } catch (IOException e) {

            remove(memberId, emitter);

            emitter.complete();

        } catch (IllegalStateException e) {

            remove(memberId, emitter);

            emitter.complete();

        }

        // 2-7. SSE 연결 객체 반환
        return emitter;
    }


    // [3] 특정 회원에게 매칭 알림 전송
    public void onMessage(String memberId) {

        // 3-1. 해당 회원의 연결 목록 조회
        List<SseEmitter> list = emitters.get(memberId);

        // 연결된 회원이 없으면 종료
        if (list == null) {
            return;
        }

        // 3-2. 해당 회원에게 알림 전송
        for (SseEmitter emitter : list) {

            try {
                emitter.send(SseEmitter.event().name("matching-updated").data("매칭 정보가 변경되었습니다."));

            // 3-3. 전송 실패 시 연결 삭제
            } catch (IOException e) {

                remove(memberId, emitter);

            } catch (IllegalStateException e) {

                remove(memberId, emitter);

            }

        }
        
    }

    // [4] 종료된 SSE 연결 정보 삭제
    private void remove(String memberId, SseEmitter emitter) {

        emitters.computeIfPresent(memberId, (id, list) -> {

            // 종료된 연결 삭제
            list.remove(emitter);

            // 연결이 없으면 회원 정보도 삭제
            if (list.isEmpty()) {

                return null;

            }

            return list;});

    }

}
