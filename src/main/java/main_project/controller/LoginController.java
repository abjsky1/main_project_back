package main_project.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.LoginDto;
import main_project.model.entity.MemberEntity;
import main_project.service.LoginService;
import main_project.service.RedisTokenService;
import main_project.util.JwtUtil;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    @PostMapping
    public MemberEntity login(@RequestBody LoginDto loginDto , HttpServletResponse response) {
        
        // 1. 서비스에게 로그인 검증 요청
        MemberEntity result = loginService.login(loginDto);

        // 2. 로그인 실패시 null 반환
        if (result == null) { return null;}

        // 3. 로그인 성공시 memberId를 이용해서 JWT Access token 생성
        String accessToken = jwtUtil.createAccessToken(result.getMemberId());
        String refreshToken = jwtUtil.createRefreshToken(result.getMemberId());

        // 4. Access Token을 쿠키에 저장
        ResponseCookie cookie = ResponseCookie
                                .from("login_member", accessToken)
                                .path("/")
                                .maxAge(Duration.ofDays(1))
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .build();
        // 5. 응답 Header에 쿠키 등록
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return result;  
        
    }

    @PostMapping ("/logout")
    public boolean logout(HttpServletResponse response) {

        // 로그인 때 생성한 login_member 쿠키를 같은 이름으로 다시 만들고 유효시간을 0으로 설정
        ResponseCookie cookie = ResponseCookie
                    .from("login_member" , "")
                    .path("/")
                    .maxAge(0)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .build();
        // 쿠키 삭제 명령 전달
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return true;
    }
}