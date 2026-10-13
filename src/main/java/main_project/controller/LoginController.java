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
import main_project.model.entity.MemberEntity;
import main_project.service.LoginService;
import main_project.service.RedisTokenService;
import main_project.util.JwtUtil;
import main_project.model.dto.MemberDto;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    // [1] 로그인
    @PostMapping 
    public boolean login(@RequestBody LoginDto loginDto , HttpServletResponse response) {
        
        // 1. 서비스에게 로그인 검증 요청 
        MemberEntity result = loginService.login(loginDto);

        // 2. 로그인 실패
        if (result == null) {return false;}

        try{
        // 3. 로그인 성공시 memberId를 이용해서 JWT에 Access token 생성 요청 
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

        // 현재 로그인한 회원 정보 조회 // 
        @GetMapping("/me")
        public MemberDto getMyInfo(@CookieValue(value = "AccessToken", required = false)String accessToken) {
            // 1. AccessToken 검증 후 회원 ID 확인
            String memberId = jwtUtil.getMemberIdFromToken(accessToken);
            // 2. 토큰이 없거나 검증에 실패하면 조회 중단
            if (memberId == null) {return null;}
            // 3. 서비스에서 회원 조회·활성 상태 확인 후 DTO 반환
            return loginService.getMyInfo(memberId);
        }

    // [2] 로그아웃
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
        return true; }

        // [3] RefreshToken 검증 후 토큰 재발급
        @PostMapping ("/reissue")
        public MemberDto reissue( @CookieValue (value = "RefreshToken" , required = false) String refreshToken , HttpServletResponse response)
        {if( refreshToken == null){ return null;}

        String memberId = jwtUtil.getMemberIdFromRefreshToken(refreshToken);

        if(memberId == null) { return null;}

        String savedRefreshToken = redisTokenService.getRefreshToken(memberId);

        if (savedRefreshToken == null || !refreshToken.equals(savedRefreshToken)) {redisTokenService.deleteRefreshToken(memberId); return null;}

        // 회원 존재·활성 상태 확인
        MemberDto member = loginService.getMyInfo(memberId);

        if (member == null) {redisTokenService.deleteRefreshToken(memberId);return null;}


        String newAccessToken = jwtUtil.createAccessToken(memberId);
        String newRefreshToken= jwtUtil.createRefreshToken(memberId);

        ResponseCookie accessCookie = ResponseCookie
                                        .from("AccessToken" , newAccessToken)
                                        .path("/")
                                        .maxAge(Duration.ofMinutes(20))
                                        .httpOnly(true)
                                        .secure(false)
                                        .sameSite("Lax")
                                        .build();
           // 9. RefreshToken 쿠키 생성: 유효기간 7일
        ResponseCookie refreshCookie = ResponseCookie
                                        .from("RefreshToken", newRefreshToken)
                                        .path("/")
                                        .maxAge(Duration.ofDays(7))
                                        .httpOnly(true)
                                        .secure(false)
                                        .sameSite("Lax")
                                        .build();

            // 10. 응답 헤더에 쿠키 등록
        redisTokenService.setRefreshToken(memberId, newRefreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE,accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE,refreshCookie.toString());


        return member;
    }

    }



