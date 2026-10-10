
package main_project.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CookieValue;

import lombok.RequiredArgsConstructor;

import main_project.model.dto.MatchingDto;

import main_project.service.MatchingService;
import main_project.service.LoginService;
import main_project.service.AuthorizationService;
import main_project.util.JwtUtil;


@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    // [추가] 로그인 회원 확인
    private final JwtUtil jwtUtil;

    // [추가] 회원 정보 확인
    private final LoginService loginService;

    // [추가] 관리자 권한 확인
    private final AuthorizationService authorizationService;


    // [1] 매칭 결과 전체 조회 (관리자)
    @GetMapping("")
    public List<MatchingDto> matchingRead(
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 관리자 권한 확인
        boolean result = adminCheck(accessToken);

        // 2. 관리자가 아니면 빈 리스트 반환
        if (result == false) {

            return new ArrayList<>();

        }

        // 3. 관리자라면 전체 매칭 조회
        return matchingService.matchingRead();

    }


    // [2] 회원별 매칭 결과 조회
    @GetMapping("/member/{memberId}")
    public List<MatchingDto> matchingMemberRead(
            @PathVariable("memberId") String memberId,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 로그인 회원 본인인지 확인
        boolean result = memberCheck(accessToken, memberId);

        // 2. 본인이 아니면 빈 리스트 반환
        if (result == false) {

            return new ArrayList<>();

        }

        // 3. 본인이라면 매칭 결과 조회
        return matchingService.matchingMemberRead(memberId);

    }


    // [3] 수출입기업(화주) 매칭 수락
    // A방식에서는 물류기업에 매칭을 요청하는 기능
    @PostMapping("/shipper/accept/{matchingId}")
    public boolean shipperAccept(
            @PathVariable("matchingId") Integer matchingId,
            @RequestParam("memberId") String memberId,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 로그인 회원 본인인지 확인
        boolean result = memberCheck(accessToken, memberId);

        // 2. 본인이 아니면 요청 실패
        if (result == false) {

            return false;

        }

        // 3. 화주 매칭 요청
        return matchingService.shipperAccept(matchingId, memberId);

    }


    // [4] 수출입기업(화주) 매칭 거절
    @PostMapping("/shipper/reject/{matchingId}")
    public boolean shipperReject(
            @PathVariable("matchingId") Integer matchingId,
            @RequestParam("memberId") String memberId,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 로그인 회원 본인인지 확인
        boolean result = memberCheck(accessToken, memberId);

        // 2. 본인이 아니면 거절 실패
        if (result == false) {

            return false;

        }

        // 3. 화주 매칭 거절
        return matchingService.shipperReject(matchingId, memberId);

    }


    // [5] 물류기업 매칭 수락
    // 관리자 승인 없이 매칭 완료
    @PostMapping("/logistics/accept/{matchingId}")
    public boolean logisticsAccept(
            @PathVariable("matchingId") Integer matchingId,
            @RequestParam("memberId") String memberId,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 로그인 회원 본인인지 확인
        boolean result = memberCheck(accessToken, memberId);

        // 2. 본인이 아니면 수락 실패
        if (result == false) {

            return false;

        }

        // 3. 물류기업 수락
        return matchingService.logisticsAccept(matchingId, memberId);

    }


    // [6] 물류기업 매칭 거절
    @PostMapping("/logistics/reject/{matchingId}")
    public boolean logisticsReject(
            @PathVariable("matchingId") Integer matchingId,
            @RequestParam("memberId") String memberId,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 로그인 회원 본인인지 확인
        boolean result = memberCheck(accessToken, memberId);

        // 2. 본인이 아니면 거절 실패
        if (result == false) {

            return false;

        }

        // 3. 물류기업 매칭 거절
        return matchingService.logisticsReject(matchingId, memberId);

    }


    // [7] 로그인 회원 본인 확인
    private boolean memberCheck(String accessToken, String memberId) {

        // 1. 로그인 정보가 없는 경우
        if (accessToken == null) {

            return false;

        }

        // 2. 로그인 정보에서 회원 번호 가져오기
        String loginMemberId = jwtUtil.getMemberIdFromToken(accessToken);

        // 3. 로그인 회원 번호가 없는 경우
        if (loginMemberId == null) {

            return false;

        }

        // 4. 회원 정보가 존재하는지 확인
        if (loginService.getMyInfo(loginMemberId) == null) {

            return false;

        }

        // 5. 로그인한 회원과 요청한 회원이 다른 경우
        if (!loginMemberId.equals(memberId)) {

            return false;

        }

        // 6. 모두 통과하면 본인 확인 성공
        return true;

    }


    // [8] 관리자 권한 확인
    private boolean adminCheck(String accessToken) {

        // 1. 로그인 정보가 없는 경우
        if (accessToken == null) {

            return false;

        }

        // 2. 로그인한 회원 번호 확인
        String loginMemberId = jwtUtil.getMemberIdFromToken(accessToken);

        if (loginMemberId == null) {

            return false;

        }

        // 3. 회원 정보 확인
        if (loginService.getMyInfo(loginMemberId) == null) {

            return false;

        }

        // 4. 관리자 권한 확인
        String adminId = authorizationService.getLoginAdminId(accessToken);

        if (adminId == null) {

            return false;

        }

        // 5. 관리자라면 true 반환
        return true;

    }

}
