package main_project.chat.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import main_project.global.util.JwtUtil;

//  =====================================================================
//  웹소켓 연결할 때 회원 확인 (day062 에는 없던 부분)
//
//  - 왜 필요한가
//      day062 는 화면이 보낸 sender(닉네임)를 그대로 믿어서 , 누구나 아무 이름으로나 보낼 수 있었음
//      메인 프로젝트는 로그인 쿠키(AccessToken)가 httpOnly 라서 프론트 JS 가 읽을 수 없음
//      → 대신 웹소켓이 처음 연결될 때(handshake) 브라우저가 쿠키를 같이 보내 줌
//      → 그 순간 서버가 쿠키로 회원을 확인해서 "연결 정보(세션)" 에 저장해 둠
//      → 이후 메시지가 올 때마다 ChatMessageController 가 저장해 둔 값을 꺼내 씀 (화면이 보낸 값은 안 믿음)
//
//  - 연결 정보에 저장하는 값 : memberId (회원 번호) 하나 (이름은 아래 상수 — ChatMessageController 도 같은 상수로 꺼냄)
//
//  - 역할 나누기 (연결할 때 인증 + 메시지마다 권한 확인)
//      연결할 때 (여기)   : "누구인지" 만 확인 — 쿠키는 이때만 받을 수 있음
//      메시지마다 (ChatService) : "이 일을 해도 되는지" 를 DB 에서 다시 확인 — 관리자인지 , 비활성 회원인지 , 내 방인지
//      구독할 때마다 (ChatChannelInterceptor → ChatService ⑩) : "이 주소를 받아도 되는지" 를 DB 에서 확인
//      → 관리자인지를 여기서 저장해 두지 않는 이유 : 연결된 채로 권한이 바뀌어도 다음 메시지부터 바로 반영하려고
//
//  - 주의 : 쿠키(토큰)는 연결할 때 한 번만 확인함
//      연결된 뒤에 쿠키가 만료(20분)되어도 , 연결이 끊길 때까지는 연결할 때 확인한 회원 번호를 씀
//      (다시 연결하면 그때의 쿠키로 다시 확인)
//  =====================================================================
@Component               // 스프링이 객체를 만들어서 WebSocketConfig 에 넣어 줌
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

//  연결 정보(세션)에 저장할 때 쓰는 이름표
//  - 넣는 곳(이 클래스)과 꺼내는 곳(ChatMessageController) 두 곳에서 같은 글자를 써야 해서 public 으로 한 곳에만 둠
    public static final String ATTR_MEMBER_ID = "memberId";

    private final JwtUtil jwtUtil;   // 토큰에서 회원 번호 꺼내기


//  연결 "전" 에 실행 — true 를 돌려주면 연결 , false 면 연결 거절
//  - request    : 연결 요청 (쿠키가 여기 들어 있음)
//  - attributes : 연결 정보(세션)에 저장할 값들을 넣는 상자
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes){

    //  1. 연결 요청에서 AccessToken 쿠키 꺼내기
    //     ServerHttpRequest 는 쿠키를 바로 꺼내는 기능이 없어서 , 원래 요청(HttpServletRequest)으로 바꿔서 꺼냄
    //     instanceof : "이 객체가 이 종류인지" 확인 + 맞으면 그 종류로 바꾼 변수(servletServerHttpRequest)를 만들어 줌
        String accessToken = null;
        if (request instanceof ServletServerHttpRequest servletServerHttpRequest) {
            Cookie[] cookies = servletServerHttpRequest.getServletRequest().getCookies();   // 쿠키가 하나도 없으면 null
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("AccessToken".equals(cookie.getName())) { accessToken = cookie.getValue(); }
                }
            }
        }

    //  2. 쿠키 속 토큰에서 회원 번호 꺼내기 (쿠키가 없거나 , 만료 · 가짜 토큰이면 null)
        String memberId = (accessToken == null) ? null : jwtUtil.getMemberIdFromToken(accessToken);

    //  3. 로그인 안 했으면 연결 거절 (401 = 로그인이 필요함)
        if (memberId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

    //  4. 연결 정보(세션)에 회원 번호 저장 → 이 연결로 오는 메시지마다 꺼내 씀
    //     (관리자인지는 저장하지 않음 — 메시지마다 ChatService 가 DB 에서 다시 확인)
        attributes.put(ATTR_MEMBER_ID, memberId);

        return true;
    }


//  연결 "후" 에 실행 — 할 일 없음 (HandshakeInterceptor 를 구현하려면 꼭 있어야 하는 메소드라 비워 둠)
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception){
    }

}
