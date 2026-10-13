package main_project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

//  =====================================================================
//  웹소켓 + STOMP 설정 (springweb day062 의 WebSocketConfig 와 같은 구조)
//
//  - 소켓 주소 : /ws-chat                    → 프론트가 처음 한 번 연결하는 곳
//                                              (프론트는 Vite 프록시를 거쳐 ws://localhost:8443/ws-chat 로 연결 — 8단계)
//  - 발행 주소 : /pub + @MessageMapping 주소  → 프론트가 메시지를 "보내는" 곳   예) /pub/chat/message
//  - 구독 주소 : /sub/...                     → 프론트가 메시지를 "받는" 곳
//      /sub/chat/room/{roomId} : 회원 화면 (내 방의 새 메시지 · 상담 시작/종료 안내)
//      /sub/chat/admin         : 관리자 화면 (모든 방의 새 메시지 → 채팅방 목록 · 안 읽음 수 갱신)
//
//  - day062 와 달라진 점 : 검사 장치 2개를 붙임
//    1. ChatHandshakeInterceptor : 연결되는 순간 로그인 쿠키(AccessToken)로 누구인지 확인 (로그인 안 했으면 연결 거절)
//    2. ChatChannelInterceptor   : 구독 · 보내기 프레임마다 검사 (남의 방 · 관리자 주소 구독 막기 , /sub 로 바로 보내기 막기)
//  =====================================================================
@Configuration                   // 스프링 설정 클래스로 등록
@EnableWebSocketMessageBroker    // STOMP 브로커(메시지 전달자) 기능 켜기
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//  연결할 때 쿠키로 회원을 확인하는 장치 (같은 config 폴더의 ChatHandshakeInterceptor)
    private final ChatHandshakeInterceptor chatHandshakeInterceptor;

//  구독 · 보내기 프레임을 검사하는 장치 (같은 config 폴더의 ChatChannelInterceptor)
    private final ChatChannelInterceptor chatChannelInterceptor;


//  구독(받기) · 발행(보내기) 주소 정하기 — day062 와 똑같음
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry){

    //  /sub 로 시작하는 주소를 구독한 화면들에게 브로커가 메시지를 전달
        registry.enableSimpleBroker("/sub");

    //  /pub 로 시작하는 주소로 온 메시지는 @MessageMapping 메소드로 보냄
    //  예) /pub/chat/message → ChatMessageController 의 @MessageMapping("/chat/message")
        registry.setApplicationDestinationPrefixes("/pub");
    }


//  소켓 주소 등록 — day062 와 같고 , 쿠키 확인 장치만 추가
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){

        registry.addEndpoint("/ws-chat")
    //  우리 화면(프론트) 주소에서 연결할 때만 받음 (day062 는 "*" = 어느 사이트든)
    //  - 브라우저는 연결 요청에 "이 페이지가 어느 주소에서 열렸는지(Origin)" 를 붙여 보냄 → 목록에 없으면 403 으로 거절
    //  - 왜 "*" 가 위험한가 : 쿠키는 포트가 달라도 같은 사이트(localhost)면 같이 보내짐
    //      → "*" 이면 localhost 의 다른 페이지(예: localhost:3000)가 내 로그인 쿠키로 몰래 연결할 수 있음
    //  - 개발 : Vite(8443)를 거쳐 들어오므로 그 주소 / 배포할 때는 실제 사이트 주소로 바꿀 것 (예: "https://macross.co.kr")
                .setAllowedOriginPatterns("http://localhost:8443", "http://127.0.0.1:8443")
    //  연결되는 순간 쿠키로 회원 확인 (ChatHandshakeInterceptor.beforeHandshake 실행)
                .addInterceptors(chatHandshakeInterceptor);
    }


//  화면 → 서버로 들어오는 STOMP 프레임(연결 · 구독 · 보내기 ...)에 검사 장치 붙이기 — day062 에는 없던 부분
//  - clientInboundChannel : 화면에서 들어오는 프레임이 지나가는 통로 → 여기를 지날 때마다 ChatChannelInterceptor.preSend 실행
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration){
        registration.interceptors(chatChannelInterceptor);
    }

}
