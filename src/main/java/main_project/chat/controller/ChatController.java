package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MessageDto;
import main_project.model.dto.RoomDto;
import main_project.service.AuthorizationService;
import main_project.service.ChatService;
import main_project.util.JwtUtil;

//  =====================================================================
//  고객센터 채팅 REST API (지난 대화 불러오기 · 상담 시작/종료 · 읽음 처리)
//  - 실시간 메시지 보내기 · 받기는 웹소켓으로 따로 처리 (6단계)
//
//  - 메소드마다 맨 앞에서 로그인 쿠키(AccessToken)로 "누가 요청했는지" 확인한 뒤 서비스에 넘김
//      1. 쿠키 속 토큰 → 회원 번호 (memberId)
//      2. 관리자인지 (isAdmin) — AuthorizationService 의 관리자 확인을 그대로 사용
//         (관리자 번호 "2" 를 여기에 또 적지 않으려고)
//    → "이 사람이 이 일을 해도 되는지" 는 ChatService 가 판단 (회원은 내 방만 , 관리자는 모든 방 ...)
//    → 나중에 AOP 로 쿠키 확인을 한 곳에 모으면 , 각 메소드의 1 · 2 번만 지우면 됨 (서비스는 그대로)
//
//  - 권한이 없거나 실패하면 null / false (AuthorizationController 와 같은 방식)
//  =====================================================================
@RestController
@RequiredArgsConstructor
@RequestMapping ("/api")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달 (관리자 목록은 /api/admin 아래라 여기서는 /api 까지만)
public class ChatController {

    private final ChatService chatService;
    private final AuthorizationService authorizationService;   // 관리자인지 확인
    private final JwtUtil jwtUtil;                             // 토큰에서 회원 번호 꺼내기


//  ① 내 채팅방 보기 (기업 회원)
//  예) GET /api/chat/rooms/me
//  - 방 정보 + 이번 상담 메시지 + 안 읽음 수 , 상담을 한 번도 안 했으면 null
//  @CookieValue : 로그인할 때 받은 출입증(AccessToken) 쿠키 (없으면 null)
    @GetMapping ("/chat/rooms/me")
    public RoomDto getMyRoom(
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 쿠키 속 토큰에서 회원 번호 꺼내기 (쿠키가 없거나 , 만료 · 가짜 토큰이면 null)
        String memberId = (accessToken == null) ? null : jwtUtil.getMemberIdFromToken(accessToken);

        // 2. 관리자인지 확인 (관리자가 아니거나 로그인 안 했으면 getLoginAdminId 가 null)
        boolean isAdmin = authorizationService.getLoginAdminId(accessToken) != null;

        // 3. 서비스에 요청
        return chatService.getMyRoom(memberId, isAdmin);
    }


//  ② 상담 시작 (on) (기업 회원)
//  예) POST /api/chat/rooms/me/open
//  - 방이 없으면 만들고 , 종료된 방이면 다시 켬 → 방 정보 + 이번 상담 메시지
//  - 감사 로그 "상담 시작" 으로 기록됨 (AuditTargets 등록표)
    @PostMapping ("/chat/rooms/me/open")
    public RoomDto openMyRoom(
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 쿠키 → 회원 번호 (① 의 1번과 같음)
        String memberId = (accessToken == null) ? null : jwtUtil.getMemberIdFromToken(accessToken);

        // 2. 관리자인지 (① 의 2번과 같음)
        boolean isAdmin = authorizationService.getLoginAdminId(accessToken) != null;

        // 3. 서비스에 요청
        return chatService.openMyRoom(memberId, isAdmin);
    }


//  ③ 상담 종료 (off) (그 방의 회원 또는 관리자)
//  예) PUT /api/chat/rooms/3/close
//  - 성공하면 true , 없는 방 · 남의 방 · 이미 종료된 방이면 false
//  - 감사 로그 "상담 종료" 로 기록됨 (AuditTargets 등록표)
//  @PathVariable : 주소 안의 {roomId} 자리 값을 꺼냄  예) /rooms/3/close → 3
    @PutMapping ("/chat/rooms/{roomId}/close")
    public boolean closeRoom(
            @PathVariable ("roomId") Integer roomId,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 쿠키 → 회원 번호 (① 의 1번과 같음)
        String memberId = (accessToken == null) ? null : jwtUtil.getMemberIdFromToken(accessToken);

        // 2. 관리자인지 (① 의 2번과 같음)
        boolean isAdmin = authorizationService.getLoginAdminId(accessToken) != null;

        // 3. 서비스에 요청
        return chatService.closeRoom(roomId, memberId, isAdmin);
    }


//  ④ 관리자 채팅방 목록 (관리자)
//  예) GET /api/admin/chat/rooms
//  - 상담 중(켜진 시각 오래된 순) → 종료(꺼진 시각 최근 순) , 관리자가 아니면 null
    @GetMapping ("/admin/chat/rooms")
    public List<RoomDto> getRooms(
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 관리자인지 (목록은 관리자만 보므로 회원 번호는 필요 없음)
        boolean isAdmin = authorizationService.getLoginAdminId(accessToken) != null;

        // 2. 서비스에 요청
        return chatService.getRooms(isAdmin);
    }


//  ⑤ 한 방의 메시지 목록 (그 방의 회원 또는 관리자)
//  예) GET /api/chat/rooms/3/messages
//  - 관리자 : 지난 상담까지 전부 , 회원 : 이번 상담만 , 없는 방 · 남의 방이면 null
    @GetMapping ("/chat/rooms/{roomId}/messages")
    public List<MessageDto> getMessages(
            @PathVariable ("roomId") Integer roomId,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 쿠키 → 회원 번호 (① 의 1번과 같음)
        String memberId = (accessToken == null) ? null : jwtUtil.getMemberIdFromToken(accessToken);

        // 2. 관리자인지 (① 의 2번과 같음)
        boolean isAdmin = authorizationService.getLoginAdminId(accessToken) != null;

        // 3. 서비스에 요청
        return chatService.getMessages(roomId, memberId, isAdmin);
    }


//  ⑥ 읽음 처리 (그 방의 회원 또는 관리자)
//  예) PUT /api/chat/rooms/3/read
//  - 성공하면 true , 없는 방 · 남의 방이면 false
//  - 감사 로그에는 기록하지 않음 (화면을 볼 때마다 불려서 로그가 너무 많아짐)
    @PutMapping ("/chat/rooms/{roomId}/read")
    public boolean markRead(
            @PathVariable ("roomId") Integer roomId,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 쿠키 → 회원 번호 (① 의 1번과 같음)
        String memberId = (accessToken == null) ? null : jwtUtil.getMemberIdFromToken(accessToken);

        // 2. 관리자인지 (① 의 2번과 같음)
        boolean isAdmin = authorizationService.getLoginAdminId(accessToken) != null;

        // 3. 서비스에 요청
        return chatService.markRead(roomId, memberId, isAdmin);
    }

}
