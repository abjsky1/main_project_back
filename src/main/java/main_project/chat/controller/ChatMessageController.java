package main_project.chat.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import main_project.chat.config.ChatHandshakeInterceptor;
import main_project.chat.dto.MessageSendDto;
import main_project.chat.service.ChatService;

//  =====================================================================
//  웹소켓 메시지 받기 (springweb day062 의 MessageController 와 같은 구조)
//
//  - 프론트가 /pub/chat/message 로 보내면 아래 메소드가 실행됨
//      (/pub 는 WebSocketConfig 의 발행 주소 , /chat/message 는 @MessageMapping 주소)
//  - day062 와 달라진 점
//      1. 보낸 사람 : 화면이 보낸 sender 대신 , 연결할 때 쿠키로 확인해 둔 회원 정보를 꺼내 씀
//      2. DB 저장  : day062 에서 [생략] 했던 부분 → ChatService.sendMessage 가 저장
//      3. 보내기   : day062 는 여기서 convertAndSend 를 했지만 , 이번에는 ChatService 가 함
//                    (상담 시작 · 종료 · 탈퇴 안내도 같은 방법으로 보내야 해서 한 곳에 모음)
//  - @Controller : @RestController 가 아님 (HTTP 응답을 돌려주는 메소드가 아니라서 — day062 와 같음)
//  - 감사 로그 AOP 는 controller 폴더 메소드를 모두 감싸지만 , 웹소켓 메시지는 HTTP 요청이 아니라서 기록하지 않고 그냥 통과함
//  =====================================================================
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;


//  ⑦ 메시지 보내기
//  예) 프론트 : client.publish({ destination: "/pub/chat/message", body: JSON.stringify({ roomId: 3, content: "문의드려요" }) })
//  - MessageSendDto : 보낸 JSON 글자 { roomId , content } 를 스프링이 객체로 바꿔서 넣어 줌
//  - SimpMessageHeaderAccessor : 이 메시지가 온 "연결" 의 정보 (연결할 때 저장해 둔 회원 정보가 들어 있음)
    @MessageMapping ("/chat/message")
    public void sendMessage(MessageSendDto messageSendDto, SimpMessageHeaderAccessor headerAccessor){

        // 1. 연결할 때 ChatHandshakeInterceptor 가 저장해 둔 회원 정보 꺼내기
        //    getSessionAttributes( ) : 연결 정보(세션) 상자 — 저장한 값이 없으면 null 일 수 있음
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();
        if (attributes == null) { return; }

        //    상자에는 무엇이든 넣을 수 있어서(Object) , 꺼낼 때 원래 종류로 바꿔 줌 — (String) : 글자로
        String memberId = (String) attributes.get(ChatHandshakeInterceptor.ATTR_MEMBER_ID);

        // 2. 서비스에 요청 (관리자 확인 → DB 저장 → 저장이 확정된 뒤 웹소켓으로 보내기까지 ChatService 가 함)
        //    관리자인지는 넘기지 않음 → ChatService 가 메시지마다 DB 에서 다시 확인
        //    저장하지 못하면(남의 방 , 종료된 방 , 빈 내용 등) 아무 일도 일어나지 않음
        chatService.sendMessage(messageSendDto, memberId);
    }

}
