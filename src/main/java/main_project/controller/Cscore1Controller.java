
package main_project.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CookieValue;

import lombok.RequiredArgsConstructor;

import main_project.model.dto.Cscore1Dto;
import main_project.model.dto.Cscore2Dto;
import main_project.model.dto.Cscore3Dto;
import main_project.model.dto.CscoreRequestDto;

import main_project.service.Cscore1Service;
import main_project.service.LoginService;
import main_project.service.AuthorizationService;

import main_project.util.JwtUtil;


@RestController
@RequestMapping("/api/cscore")
@RequiredArgsConstructor
public class Cscore1Controller {

    private final Cscore1Service cscore1Service;

    // [추가] 로그인 회원 확인
    private final JwtUtil jwtUtil;

    // [추가] 회원 정보 확인
    private final LoginService loginService;

    // [추가] 관리자 권한 확인
    private final AuthorizationService authorizationService;


    // [1] 화주 매칭 조건 등록
    @PostMapping("")
    public boolean cscoreWrite(
            @RequestBody CscoreRequestDto requestDto,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 전달받은 데이터 확인
        if (requestDto == null) {

            return false;

        }

        if (requestDto.getCscore1Dto() == null
                || requestDto.getCscore2Dto() == null
                || requestDto.getCscore3Dto() == null) {

            return false;

        }

        // 2. 조건을 등록하려는 회원 번호 확인
        String memberId = requestDto.getCscore1Dto().getMemberId();

        // 3. 로그인한 회원 본인인지 확인
        boolean result = memberCheck(accessToken, memberId);

        if (result == false) {

            return false;

        }

        // 4. 화주 매칭 조건 등록
        // Cscore1Service에서 조건 저장 후 자동 매칭 실행
        return cscore1Service.cscoreWrite(

                requestDto.getCscore1Dto(),

                requestDto.getCscore2Dto(),

                requestDto.getCscore3Dto()

        );

    }


    // [2] 화주 매칭 조건 목록 조회
    // memberId 생략 시 관리자 전체 조회
    @GetMapping("")
    public List<Cscore1Dto> cscoreRead(
            @RequestParam(name = "memberId", required = false) String memberId,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. memberId가 없으면 관리자만 전체 조회 가능
        if (memberId == null) {

            boolean result = adminCheck(accessToken);

            if (result == false) {

                return new ArrayList<>();

            }

        } else {

            // 2. 회원별 조회는 본인 또는 관리자만 가능
            boolean memberResult = memberCheck(accessToken, memberId);

            boolean adminResult = adminCheck(accessToken);

            if (memberResult == false && adminResult == false) {

                return new ArrayList<>();

            }

        }

        // 3. 권한 확인 후 조건 목록 조회
        return cscore1Service.cscoreRead(memberId);

    }


    // [3] Cscore1 개별 조회
    @GetMapping("/{cscore1Id}")
    public Cscore1Dto cscoreFindById(
            @PathVariable("cscore1Id") Integer cscore1Id,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 해당 조건을 조회할 권한이 있는지 확인
        boolean result = conditionCheck(accessToken, cscore1Id);

        if (result == false) {

            return null;

        }

        // 2. Cscore1 조회
        return cscore1Service.cscoreFindByID(cscore1Id);

    }


    // [4] Cscore1에 연결된 Cscore2 조회
    @GetMapping("/{cscore1Id}/cscore2")
    public Cscore2Dto cscore2Read(
            @PathVariable("cscore1Id") Integer cscore1Id,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 해당 조건을 조회할 권한이 있는지 확인
        boolean result = conditionCheck(accessToken, cscore1Id);

        if (result == false) {

            return null;

        }

        // 2. Cscore2 조회
        return cscore1Service.cscore2Read(cscore1Id);

    }


    // [5] Cscore1에 연결된 Cscore3 조회
    @GetMapping("/{cscore1Id}/cscore3")
    public Cscore3Dto cscore3Read(
            @PathVariable("cscore1Id") Integer cscore1Id,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 해당 조건을 조회할 권한이 있는지 확인
        boolean result = conditionCheck(accessToken, cscore1Id);

        if (result == false) {

            return null;

        }

        // 2. Cscore3 조회
        return cscore1Service.cscore3Read(cscore1Id);

    }


    // [6] 화주 매칭 조건 삭제
    @DeleteMapping("/{cscore1Id}")
    public boolean cscoreDelete(
            @PathVariable("cscore1Id") Integer cscore1Id,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 삭제할 화주 조건 조회
        Cscore1Dto cscore1Dto = cscore1Service.cscoreFindByID(cscore1Id);

        if (cscore1Dto == null) {

            return false;

        }

        // 2. 로그인 회원과 조건 소유자 확인
        boolean result = memberCheck(accessToken, cscore1Dto.getMemberId());

        if (result == false) {

            return false;

        }

        // 3. 본인 조건이라면 삭제 실행
        // 요청 또는 처리된 매칭이 있는지 Service에서 검사
        return cscore1Service.cscoreDelete(cscore1Id);

    }


    // [7] 로그인 회원 본인 확인
    private boolean memberCheck(String accessToken, String memberId) {

        // 1. 로그인 정보가 없으면 false
        if (accessToken == null || memberId == null) {

            return false;

        }

        // 2. 로그인 정보에서 회원 번호 가져오기
        String loginMemberId = jwtUtil.getMemberIdFromToken(accessToken);

        if (loginMemberId == null) {

            return false;

        }

        // 3. 존재하는 활성 회원인지 확인
        if (loginService.getMyInfo(loginMemberId) == null) {

            return false;

        }

        // 4. 로그인 회원과 요청한 회원이 다르면 false
        if (!loginMemberId.equals(memberId)) {

            return false;

        }

        // 5. 모든 조건 통과
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

        return true;

    }


    // [9] 화주 조건 소유자 또는 관리자 확인
    private boolean conditionCheck(String accessToken, Integer cscore1Id) {

        // 1. 화주 매칭 조건 조회
        Cscore1Dto cscore1Dto = cscore1Service.cscoreFindByID(cscore1Id);

        // 2. 조건이 존재하지 않으면 false
        if (cscore1Dto == null) {

            return false;

        }

        // 3. 관리자라면 조회 가능
        boolean adminResult = adminCheck(accessToken);

        if (adminResult == true) {

            return true;

        }

        // 4. 일반 회원이라면 조건 소유자인지 확인
        boolean memberResult = memberCheck(accessToken, cscore1Dto.getMemberId());

        if (memberResult == false) {

            return false;

        }

        return true;

    }

}
