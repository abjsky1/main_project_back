package main_project.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
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
    public boolean login(@RequestBody LoginDto loginDto , HttpServletResponse response) {
        
        // 1. 서비스에게 로그인 검증 요청 
        MemberEntity result = loginService.login(loginDto);

        // 2. 로그인 실패
        if (result == null) {return false;}

        try{
        // 3. 로그인 성공시 memberId를 이용해서 JWT Access token 생성
        String accessToken = jwtUtil.createAccessToken(result.getMemberId());
        String refreshToken = jwtUtil.createRefreshToken(result.getMemberId());

    

           // 4. Access Token을 쿠키에 저장
        ResponseCookie accesscookie = ResponseCookie
                                .from("AccessToken", accessToken)
                                .path("/")
                                .maxAge(Duration.ofMinutes(20))
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .build();


        // 5. Refresh Token을 쿠키에 저장
        ResponseCookie refreshcookie = ResponseCookie
                                .from("RefreshToken", refreshToken)
                                .path("/")
                                .maxAge(Duration.ofDays(7))
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .build(); 

        // 6. Refresh Token Redis 저장
        redisTokenService.setRefreshToken(result.getMemberId(), refreshToken);

        // 7. 응답 Header에 쿠키 등록
        response.addHeader(HttpHeaders.SET_COOKIE, accesscookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshcookie.toString());


    //  교수님 질문 : 쿠키 저장됐는지 검증하고 boolean 반환하는 방법.
        return true; } catch ( Exception e ) { e.printStackTrace(); return false;}
    }

    @PostMapping ("/logout")
    public boolean logout(@CookieValue(value = "AccessToken", required = false) String accessToken, HttpServletResponse response) {

        // 1. AcessToken이 존재하는 경우
        if (accessToken != null) {

        // 2. AccessToken 에서 memberId 추출
        String memberId = jwtUtil.getMemberIdFromToken(accessToken);

        // 3. memberId가 정상적으로 추출 -> Redis의 RefreshToken 삭제
        if (memberId != null) {
            redisTokenService.deleteRefreshToken(memberId);
        }
}

        // 로그인 때 생성한 login_member 쿠키를 같은 이름으로 다시 만들고 유효시간을 0으로 설정 (AccessToken 쿠키 삭제)
        ResponseCookie accesscookie = ResponseCookie
                    .from("AccessToken" , "")
                    .path("/")
                    .maxAge(0)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .build();

        // 로그인 때 생성한 login_member 쿠키를 같은 이름으로 다시 만들고 유효시간을 0으로 설정 (RefreshToken 쿠키 삭제)
        ResponseCookie refreshcookie = ResponseCookie
                    .from("RefreshToken" , "")
                    .path("/")
                    .maxAge(0)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .build();

        // 브라우저에게 쿠키 만료 응답 헤더 전달
        response.addHeader(HttpHeaders.SET_COOKIE, accesscookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshcookie.toString());
        return true;


    }
}

