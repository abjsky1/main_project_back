package main_project.chat.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import main_project.chat.service.ChatService;

//  =====================================================================
//  웹소켓 프레임 검사 — 남의 채팅방 엿듣기 · 가짜 메시지 막기 (day062 에는 없던 부분)
//
//  - 왜 필요한가
//      ChatHandshakeInterceptor 는 연결할 때 "로그인한 회원인지" 만 확인함
//      → 로그인만 하면 아무 주소나 구독하거나 , 아무 주소로나 보낼 수 있었음
//        1. 구독 : 회원 A 가 /sub/chat/room/5 (회원 B 의 방) 나 /sub/chat/admin (모든 방) 을 구독하면 남의 대화가 다 보임
//        2. 보내기 : /sub/... 주소로 바로 보내면 브로커가 DB 저장 없이 그대로 구독자에게 뿌림
//                    → 남의 화면에 가짜 메시지("고객센터: ...")를 띄울 수 있음
//
//  - 언제 실행되나 : 화면이 보낸 STOMP 프레임(연결 · 구독 · 보내기 ...)이 서버 안으로 들어올 때마다 , @MessageMapping 보다 먼저
//
//  - 검사하는 것 2가지 (나머지 프레임은 그대로 통과)
//      구독(SUBSCRIBE) : ChatService.canSubscribe 가 확인 — 관리자 주소는 관리자만 , 방 주소는 그 방 회원 또는 관리자만
//      보내기(SEND)    : /pub 주소로만 (ChatMessageController → ChatService 를 거쳐 권한 확인 · DB 저장되는 길)
//                        + 회원 1명당 1초에 5개까지 (넘으면 버림 — Rate Limiting)
//
//  - 통과 못 하면 null 을 돌려줌 → 그 프레임을 버림 (구독이 안 되니 그 주소의 메시지를 못 받고 , 보낸 메시지는 아무 데도 안 감)
//
//  - 역할 나누기 (B 방식 : 연결할 때 인증 + 매번 권한 확인)
//      연결할 때   (ChatHandshakeInterceptor) : 누구인지 (쿠키)
//      구독할 때   (여기 → ChatService ⑩)      : 이 주소를 받아도 되는지 (Redis 캐시 , 없으면 DB)
//      보낼 때     (ChatService ⑦)             : 이 방에 보내도 되는지 (Redis 캐시 , 없으면 DB)
//  =====================================================================
@Component   // 스프링이 객체를 만들어서 WebSocketConfig 에 넣어 줌
public class ChatChannelInterceptor implements ChannelInterceptor {

//  보내기 속도 제한 — 회원 1명당 1초에 몇 개까지 받을지
//  - 사람이 빠르게 쳐도 1초에 2~3개 정도라 5개면 넉넉함 (자음 하나씩 연속으로 보내는 것 같은 도배 · 매크로를 막음)
    private static final int SEND_LIMIT_PER_SECOND = 5;
//  - Redis 키 : chat:rate:{회원번호} (ChatService ⑫ 가 서버 시작 때 "chat:" 키를 같이 비움)
    private static final String RATE_KEY_PREFIX = "chat:rate:";

    private final ChatService chatService;   // 구독해도 되는지 확인 (⑩ canSubscribe)

    private final StringRedisTemplate stringRedisTemplate;   // 보내기 횟수 세기


//  생성자 — 다른 클래스처럼 @RequiredArgsConstructor 를 쓰지 않고 직접 적은 이유 : @Lazy 를 붙이려고
//  - @Lazy : ChatService 를 지금 바로 만들지 않고 , 처음 쓸 때(첫 구독) 가져옴
//  - 왜 필요한가 : 스프링이 객체를 만드는 순서가 서로 물려 있음 (순환 참조)
//      WebSocketConfig → 이 클래스 → ChatService → 웹소켓 보내기 도구(SimpMessageSendingOperations) → WebSocketConfig 설정이 끝나야 만들어짐
//      → 서로 상대가 먼저 만들어지길 기다려서 서버가 켜지지 않음 → 한쪽을 "나중에 가져오기" 로 바꿔서 끊음
//  (Lombok 의 @RequiredArgsConstructor 는 필드의 @Lazy 를 생성자로 옮겨 주지 않아서 직접 적음)
    public ChatChannelInterceptor(@Lazy ChatService chatService, StringRedisTemplate stringRedisTemplate){
        this.chatService = chatService;
        this.stringRedisTemplate = stringRedisTemplate;
    }


//  STOMP 프레임이 서버 안으로 들어가기 "전" 에 실행 — 돌려준 message 가 다음 단계로 가고 , null 이면 여기서 버림
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel){

    //  1. 프레임 정보 꺼내기 (명령 종류 · 주소 · 연결 정보)
    //     StompHeaderAccessor : 프레임의 머리(header)를 읽기 쉽게 해 주는 도구
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        String destination = accessor.getDestination();   // 구독 · 보내기 주소 (없는 프레임이면 null)

    //  2. 보내기(SEND) : /pub 주소만 통과 (WebSocketConfig 의 발행 주소)
    //     /pub/chat/message → ChatMessageController 가 받아서 ChatService 가 권한 확인 · DB 저장 뒤에 보냄
    //     /sub/... 로 바로 보내면 확인 없이 남의 화면에 뜨므로 버림
        if (command == StompCommand.SEND) {
            if (destination == null || !destination.startsWith("/pub/")) { return null; }

        //  2-1. 너무 빨리 보내면 버림 (회원 1명당 1초에 5개까지)
        //       연결할 때 ChatHandshakeInterceptor 가 저장해 둔 회원 번호 꺼내기 (ChatMessageController 의 1번과 같음)
        //       회원 번호가 없으면 여기서는 통과 → ChatService 가 저장하지 않음
            Map<String, Object> attributes = accessor.getSessionAttributes();
            String memberId = (attributes == null) ? null : (String) attributes.get(ChatHandshakeInterceptor.ATTR_MEMBER_ID);
            if (memberId == null) { return message; }

        //       increment : Redis 의 INCR — 숫자를 1 올리고 올린 값을 돌려줌 (키가 없으면 0 에서 시작 → 1)
        //       처음(1)일 때만 1초 뒤에 사라지게 → 1초가 지나면 키가 없어져서 다시 1부터 셈
            String rateKey = RATE_KEY_PREFIX + memberId;
            Long sendCount = stringRedisTemplate.opsForValue().increment(rateKey);
            if (sendCount != null && sendCount == 1) {
                stringRedisTemplate.expire(rateKey, Duration.ofSeconds(1));
            }
        //       1초 안에 6번째부터는 버림 (DB 저장 · 웹소켓 전달 없음 — 화면에도 말풍선이 뜨지 않음)
            if (sendCount != null && sendCount > SEND_LIMIT_PER_SECOND) { return null; }

            return message;
        }

    //  3. 구독(SUBSCRIBE)이 아니면 그대로 통과 (연결 · 연결 끊기 · 구독 취소 등)
        if (command != StompCommand.SUBSCRIBE) { return message; }

    //  4. 연결할 때 ChatHandshakeInterceptor 가 저장해 둔 회원 번호 꺼내기 (ChatMessageController 의 1번과 같음)
        Map<String, Object> attributes = accessor.getSessionAttributes();
        String memberId = (attributes == null) ? null : (String) attributes.get(ChatHandshakeInterceptor.ATTR_MEMBER_ID);

    //  5. 이 주소를 구독해도 되는지 확인 (ChatService ⑩ — 회원 · 방은 Redis 캐시로) → 안 되면 구독 요청 버림
        if (!chatService.canSubscribe(destination, memberId)) { return null; }

        return message;
    }

}
