package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MatchingDto;
import main_project.service.MatchingService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController 
@RequestMapping ("/api/matching")
@RequiredArgsConstructor 
public class MatchingController {

    private final MatchingService matchingService;

    // 매칭 실행
    @PostMapping("/run/{cscore1Id}")
    public boolean matchingWrite(
            @PathVariable("cscore1Id") Integer cscore1Id) {

        return matchingService.matchingWrite(cscore1Id);

    }

    // 매칭 결과 전체 조회
    @GetMapping("")
    public List<MatchingDto> matchingRead() {
        return matchingService.matchingRead();
    }

    // 매칭 승인(관리자)
    @PostMapping("/approve/{matchingId}")
    public boolean matchingApprove( 
        @PathVariable ("matchingId") Integer matchingId) {

            return matchingService.matchingApprove(matchingId);

        }

    // 매칭 반려(관라자)
    @PostMapping("/reject/{matchingId}")
    public boolean matchingReject( 
        @PathVariable ("matchingId") Integer matchingId) {

            return matchingService.matchingReject(matchingId);

        }

    // 회원별 승인 매칭 조회
    @GetMapping("/member/{memberId}")
    public List<MatchingDto> matchingMemberRead(
        @PathVariable("memberId") String memberId) {

        return matchingService.matchingMemberRead(memberId);

    }
    
    // 수출입기업(화주) 매칭 수락
    @PostMapping("/shipper/accept/{matchingId}")
    public boolean shipperAccept(
        @PathVariable ("matchingId") Integer matchingId) {

            return matchingService.shipperAccept(matchingId);

        }
    
    // 수출입기업 매칭 거절
    @PostMapping("/shipper/reject/{matchingId}")
    public boolean shipperReject(
        @PathVariable ("matchingId") Integer matchingId) {

            return matchingService.shipperReject(matchingId);

        }

    // 물류기업 매칭 수락
    @PostMapping("/logistics/accept/{matchingId}")
    public boolean logisticsAccept(
            @PathVariable("matchingId") Integer matchingId) {

        return matchingService.logisticsAccept(matchingId);
    }


    // 물류기업 매칭 거절
    @PostMapping("/logistics/reject/{matchingId}")
    public boolean logisticsReject(
            @PathVariable("matchingId") Integer matchingId) {

        return matchingService.logisticsReject(matchingId);
    }
    
}
