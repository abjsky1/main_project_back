
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
import main_project.model.dto.MatchingPageDto;
import main_project.model.entity.MatchingEntity;
import main_project.model.repository.MatchingRepository;
import main_project.service.MatchingService;
import main_project.service.MatchingSseService;
import main_project.service.LoginService;
import main_project.service.AuthorizationService;
import main_project.util.JwtUtil;


@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    // [추가] 실시간 매칭 알림 서비스
    private final MatchingSseService matchingSseService;

    // [추가] 매칭 상대방 회원 정보 조회
    private final MatchingRepository matchingRepository;

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
        if (result == false) {return new ArrayList<>();}

        // 3. 관리자라면 전체 매칭 조회
        return matchingService.matchingRead();

    }

    // [1-1] 관리자 매칭 관리 화면 — 상태별 건수 5개 + 고른 상태의 매칭 한 페이지(50건)를 한 번에
    //  예) GET /api/matching/admin?status=completed&page=0
    //  - status : all(전체) / linked(자동 매칭 연결) / requested(물류 수락 대기) / completed(성사) / failed(실패) , 안 보내면 all
    //  - page   : 몇 번째 페이지 (0 부터 — 0 = 1페이지) , 안 보내면 0
    //             defaultValue : 값이 안 오면 쓸 기본값 (springweb day039 RestController2 와 같은 방식)
    //  - 관리자가 아니면 null (화면은 "관리자만 볼 수 있습니다")
    //  (예전에 상태마다 따로 있던 /recommended , /shipper-requested , /completed , /failed 4개를 이 주소 하나로 합침)
    @GetMapping("/admin")
    public MatchingPageDto matchingAdminRead(
            @RequestParam(name = "status", required = false, defaultValue = "all") String status,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @CookieValue(value = "AccessToken", required = false) String accessToken) {

        // 1. 관리자 권한 확인 (아니면 null)
        if (!adminCheck(accessToken)) {return null;}

        // 2. 관리자라면 상태별 건수 + 그 상태의 한 페이지 조회
        return matchingService.matchingAdminRead(status, page);

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
        boolean matchingResult = matchingService.shipperAccept(matchingId, memberId);

        // 4. 매칭 요청 성공 시 알림 전송
        if (matchingResult) {

            matchingNotice(matchingId);

        }

        return matchingResult;

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
        boolean matchingResult = matchingService.shipperReject(matchingId, memberId);

        // 4. 거절 성공 시 화주 알림 갱신
        if (matchingResult) {

            matchingSseService.onMessage(memberId);

        }

        return matchingResult;

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
        boolean matchingResult = matchingService.logisticsAccept(matchingId, memberId);

        // 4. 수락 성공 시 양쪽 회원에게 알림
        if (matchingResult) {

            matchingNotice(matchingId);

        }

        return matchingResult;

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
        boolean matchingResult = matchingService.logisticsReject(matchingId, memberId);

        // 4. 거절 성공 시 양쪽 회원에게 알림
        if (matchingResult) {

            matchingNotice(matchingId);

        }

        return matchingResult;

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

    private void matchingNotice(Integer matchingId) {

        // 1. 매칭 정보 조회
        MatchingEntity matchingEntity = matchingRepository.findById(matchingId).orElse(null);

        // 2. 매칭 정보가 없으면 종료
        if (matchingEntity == null) {
            
            return;

        }

        // 3. 화주 회원 ID 조회
        String shipperMemberId = matchingEntity
                .getCscore1Entity()
                .getMemberEntity()
                .getMemberId();

        // 4. 물류기업 회원 ID 조회
        String logisticsMemberId = matchingEntity
                .getLscore1Entity()
                .getMemberEntity()
                .getMemberId();

        // 5. 화주에게 알림 전송
        matchingSseService.onMessage(shipperMemberId);

        // 6. 물류기업에게 알림 전송
        matchingSseService.onMessage(logisticsMemberId);

    }

}
