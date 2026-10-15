
package main_project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import main_project.service.MatchingSseService;
import main_project.service.LoginService;
import main_project.util.JwtUtil;

@RestController
@RequestMapping("/api/matchings")
@RequiredArgsConstructor
public class MatchingSseController {

    // [1] 서비스 연결
    private final MatchingSseService matchingSseService;
    private final JwtUtil jwtUtil;
    private final LoginService loginService;


    // [2] 매칭 알림 SSE 구독
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @CookieValue(value = "AccessToken", required = false)
            String accessToken) {

        // 2-1. 로그인 토큰에서 회원 ID 가져오기
        String memberId = jwtUtil.getMemberIdFromToken(accessToken);

        // 2-2. 로그인하지 않은 경우
        if (memberId == null) {

            // throw new : 예외를 발생시키면서 실행 중단
            throw new ResponseStatusException(

                HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 2-3. 회원이 없거나 비활성 상태인 경우
        if (loginService.getMyInfo(memberId) == null) {

            throw new ResponseStatusException(

                HttpStatus.UNAUTHORIZED, "회원 정보를 확인할 수 없습니다.");

        }

        // 2-4. 로그인 회원의 SSE 연결 생성
        return matchingSseService.subscribe(memberId);

    }
    
}
