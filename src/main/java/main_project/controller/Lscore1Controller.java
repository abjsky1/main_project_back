
package main_project.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;

import lombok.RequiredArgsConstructor;

import main_project.model.dto.Lscore1Dto;
import main_project.model.dto.Lscore2Dto;
import main_project.model.dto.Lscore3Dto;
import main_project.model.dto.LscoreRequestDto;

import main_project.service.Lscore1Service;
import main_project.service.LoginService;
import main_project.service.AuthorizationService;

import main_project.util.JwtUtil;


@RestController
@RequestMapping("/api/lscore")
@RequiredArgsConstructor
public class Lscore1Controller {

    private final Lscore1Service lscore1Service;

    // [추가] 로그인 회원 확인
    private final JwtUtil jwtUtil;

    // [추가] 회원 정보 확인
    private final LoginService loginService;

    // [추가] 관리자 권한 확인
    private final AuthorizationService authorizationService;


    // [1] 물류업체 매칭 조건 등록
    @PostMapping("")
    public boolean lscoreWrite(
            @RequestBody LscoreRequestDto requestDto,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 전달받은 데이터 확인
        if (requestDto == null) {

            return false;

        }

        if (requestDto.getLscore1Dto() == null
                || requestDto.getLscore2Dto() == null
                || requestDto.getLscore3Dto() == null) {

            return false;

        }

        // 2. 조건을 등록하려는 회원 번호 확인
        String memberId = requestDto.getLscore1Dto().getMemberId();

        // 3. 로그인한 회원 본인인지 확인
        boolean result = memberCheck(accessToken, memberId);

        if (result == false) {

            return false;

        }

        // 4. 물류기업 매칭 조건 등록
        // Lscore1Service에서 조건 저장 후 자동 매칭 실행
        return lscore1Service.lscoreWrite(

                requestDto.getLscore1Dto(),

                requestDto.getLscore2Dto(),

                requestDto.getLscore3Dto()

        );

    }


    // [2] 물류업체 매칭 조건 목록 조회
    // memberId 생략 시 관리자 전체 조회
    @GetMapping("")
    public List<Lscore1Dto> lscoreRead(
            @RequestParam(name = "memberId", required = false) String memberId,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 회원 번호가 없으면 관리자만 전체 조회 가능
        if (memberId == null) {

            boolean result = adminCheck(accessToken);

            if (result == false) {

                return new ArrayList<>();

            }

        } else {

            // 2. 회원 번호가 있으면 본인 또는 관리자 확인
            boolean memberResult = memberCheck(accessToken, memberId);

            boolean adminResult = adminCheck(accessToken);

            if (memberResult == false && adminResult == false) {

                return new ArrayList<>();

            }

        }

        // 3. 권한 확인 후 매칭 조건 조회
        return lscore1Service.lscoreRead(memberId);

    }


    // [3] Lscore1 개별 조회
    @GetMapping("/{lscore1Id}")
    public Lscore1Dto lscoreFindById(
            @PathVariable("lscore1Id") Integer lscore1Id,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 해당 조건을 조회할 권한이 있는지 확인
        boolean result = conditionCheck(accessToken, lscore1Id);

        if (result == false) {

            return null;

        }

        // 2. Lscore1 조회
        return lscore1Service.lscoreFindById(lscore1Id);

    }


    // [4] Lscore1에 연결된 Lscore2 조회
    @GetMapping("/{lscore1Id}/lscore2")
    public Lscore2Dto lscore2Read(
            @PathVariable("lscore1Id") Integer lscore1Id,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 조회 권한 확인
        boolean result = conditionCheck(accessToken, lscore1Id);

        if (result == false) {

            return null;

        }

        // 2. Lscore2 조회
        return lscore1Service.lscore2Read(lscore1Id);

    }


    // [5] Lscore1에 연결된 Lscore3 조회
    @GetMapping("/{lscore1Id}/lscore3")
    public Lscore3Dto lscore3Read(
            @PathVariable("lscore1Id") Integer lscore1Id,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 조회 권한 확인
        boolean result = conditionCheck(accessToken, lscore1Id);

        if (result == false) {

            return null;

        }

        // 2. Lscore3 조회
        return lscore1Service.lscore3Read(lscore1Id);

    }


    // [6] 물류업체 매칭 조건 삭제
    @DeleteMapping("/{lscore1Id}")
    public boolean lscoreDelete(
            @PathVariable("lscore1Id") Integer lscore1Id,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 삭제할 물류업체 조건 조회
        Lscore1Dto lscore1Dto =
                lscore1Service.lscoreFindById(lscore1Id);

        // 2. 조건이 존재하지 않으면 삭제 실패
        if (lscore1Dto == null) {

            return false;

        }

        // 3. 로그인한 회원이 조건 소유자인지 확인
        boolean result = memberCheck(accessToken, lscore1Dto.getMemberId());

        if (result == false) {

            return false;

        }

        // 4. 본인 조건이라면 삭제 실행
        // 요청되거나 처리된 매칭이 있는지는 Service에서 확인
        return lscore1Service.lscoreDelete(lscore1Id);

    }


    // [7] 로그인 회원 본인 확인
    private boolean memberCheck(String accessToken, String memberId) {

        // 1. 로그인 정보 또는 회원 번호가 없으면 false
        if (accessToken == null || memberId == null) {

            return false;

        }

        // 2. 로그인 정보에서 회원 번호 가져오기
        String loginMemberId = jwtUtil.getMemberIdFromToken(accessToken);

        if (loginMemberId == null) {

            return false;

        }

        // 3. 회원이 존재하고 활성화 상태인지 확인
        if (loginService.getMyInfo(loginMemberId) == null) {

            return false;

        }

        // 4. 로그인 회원과 요청 회원 비교
        if (!loginMemberId.equals(memberId)) {

            return false;

        }

        // 5. 본인 확인 성공
        return true;

    }


    // [8] 관리자 권한 확인
    private boolean adminCheck(String accessToken) {

        // 1. 기존 관리자 권한 확인 메서드 사용
        String adminId = authorizationService.getLoginAdminId(accessToken);

        // 2. 관리자가 아니면 false
        if (adminId == null) {

            return false;

        }

        // 3. 관리자 확인 성공
        return true;

    }


    // [9] 물류업체 조건 소유자 또는 관리자 확인
    private boolean conditionCheck(String accessToken, Integer lscore1Id) {

        // 1. 물류업체 매칭 조건 조회
        Lscore1Dto lscore1Dto = lscore1Service.lscoreFindById(lscore1Id);

        // 2. 조건이 존재하지 않으면 false
        if (lscore1Dto == null) {

            return false;

        }

        // 3. 관리자라면 조회 가능
        boolean adminResult = adminCheck(accessToken);

        if (adminResult == true) {

            return true;

        }

        // 4. 일반 회원이라면 조건 소유자 확인
        boolean memberResult = memberCheck(accessToken, lscore1Dto.getMemberId());

        if (memberResult == false) {

            return false;

        }

        // 5. 조회 권한 확인 성공
        return true;

    }

}
