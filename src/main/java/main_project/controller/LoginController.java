package main_project.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.LoginDto;
import main_project.model.dto.MemberDto;
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

    // [1] 로그인 + 쿠키 ( 인증 성공시 JWT 쿠키 2개 발급 + 회원 정보 반환 / 실패시 null )
    @PostMapping
    public MemberDto login(@RequestBody LoginDto loginDto , HttpServletResponse response) {

        // 1. 서비스에게 로그인 검증 요청
        MemberDto result = loginService.login(loginDto);

        // 2. 로그인 실패
        if (result == null) {return null;}

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


        // 8. 로그인한 회원 정보 반환 (프론트가 이 값으로 화면의 로그인 상태를 만듦)
        return result; } catch ( Exception e ) { e.printStackTrace(); return null;}
    }

    // [2] 내 정보 조회 + 쿠키 ( 이미 로그인된 회원이 내 정보 요청 , 새로고침 후 로그인 상태 복구용 )
    @GetMapping ("/me")
    public MemberDto getMyInfo(@CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 쿠키가 없으면 비로그인
        if (accessToken == null) { return null; }

        // 2. 쿠키에 저장된 token 으로 회원 번호 찾기 (만료되었거나 가짜 토큰이면 null)
        String memberId = jwtUtil.getMemberIdFromToken(accessToken);
        if (memberId == null) { return null; }

        // 3. 로그인 중이면 서비스에게 회원 정보 요청
        return loginService.getMyInfo(memberId);
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

