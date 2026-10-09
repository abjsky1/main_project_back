package main_project.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.MypageDto;
import main_project.model.dto.PasswordChangeDto;
import main_project.model.dto.WithdrawDto;
import main_project.service.MypageService;
import main_project.service.RedisTokenService;
import main_project.util.JwtUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/mypage")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
public class MypageController {

    private final MypageService mypageService;

    private final JwtUtil jwtUtil;

    private final RedisTokenService redisTokenService;

    //  내 정보 수정 (이름 · 주소)
    //  예) PUT /api/mypage/dcdc8696-...   body { "managerName": "김승영", "companyAddress": "서울특별시 광진구 자양동" }
    //  @CookieValue : 로그인할 때 받은 출입증(AccessToken) 쿠키 → 쿠키 속 회원과 주소의 회원이 같을 때만 수정
    @PutMapping ("/{memberId}")
    public boolean infoUpdate(
            @PathVariable ("memberId") String memberId,
            @RequestBody MypageDto mypageDto,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        return mypageService.infoUpdate(memberId, mypageDto, accessToken);
    }

    //  비밀번호 변경 , 성공하면 true
    //  예) PUT /api/mypage/password   body { "currentPassword": "지금 비밀번호", "newPassword": "새 비밀번호" }
    //  - 바꿀 회원은 쿠키(AccessToken) 속 회원 번호로 정함 (주소에 회원 번호를 받지 않음)
    //  - 실패 : 쿠키 없음 · 만료 / 현재 비밀번호 틀림 / 새 비밀번호가 6~20자가 아님 / 새 비밀번호가 지금과 같음
    //  - 입력값 검사까지 모두 서비스(MypageService.changePassword)가 함
    //    → 나중에 다른 곳에서 서비스를 바로 불러 써도 똑같이 안전함
    //  ※ 위의 PUT /{memberId} 와 주소 모양이 겹치지만 , 스프링은 글자가 정해진 주소(/password)를 먼저 고름
    @PutMapping ("/password")
    public boolean changePassword(
            @RequestBody PasswordChangeDto passwordChangeDto,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        return mypageService.changePassword(accessToken, passwordChangeDto);
    }

    //  회원 탈퇴 (비밀번호 확인 → 회원 삭제 → 로그아웃 처리) , 성공하면 true
    //  예) POST /api/mypage/withdraw   body { "userPassword": "입력한 비밀번호" }
    //  - 탈퇴할 회원은 쿠키(AccessToken) 속 회원 번호로 정함 (주소에 회원 번호를 받지 않음)
    //  - DELETE 대신 POST 를 쓴 이유 : 비밀번호를 body 로 보내야 하는데 , DELETE 요청은 body 를 보내기 불편함
    //  - HttpServletResponse : 응답에 쿠키 삭제 헤더를 넣기 위해 받음 (LoginController 의 logout 과 같음)
    @PostMapping ("/withdraw")
    public boolean withdraw(
            @RequestBody WithdrawDto withdrawDto,
            @CookieValue (name = "AccessToken", required = false) String accessToken,
            HttpServletResponse response){

        // 1. 서비스에게 탈퇴 요청 (본인 확인 + 비밀번호 확인 + 로그 옮기기 + 회원 삭제)
        boolean result = mypageService.withdraw(accessToken, withdrawDto.getUserPassword());
        if (result == false) { return false; }

        // ---- 여기부터는 DB 에서 회원 삭제가 끝난 뒤 : 로그아웃과 같은 처리 ----

        // 2. 레디스에 저장된 RefreshToken 삭제 (토큰 속 회원 번호 = 레디스 키)
        String memberId = jwtUtil.getMemberIdFromToken(accessToken);
        redisTokenService.deleteRefreshToken(memberId);

        // 3. 쿠키 2개 삭제
        //    로그인 때 만든 쿠키와 같은 이름으로 유효시간 0 인 쿠키를 보내면 브라우저가 지움 (logout 과 같은 방법)
        ResponseCookie accessCookie = ResponseCookie
                    .from("AccessToken", "")
                    .path("/")
                    .maxAge(0)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .build();

        ResponseCookie refreshCookie = ResponseCookie
                    .from("RefreshToken", "")
                    .path("/")
                    .maxAge(0)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .build();

        // 4. 응답 헤더에 쿠키 삭제 등록
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return true;
    }

}
