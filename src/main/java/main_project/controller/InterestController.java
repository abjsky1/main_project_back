package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.InterestDto;
import main_project.service.InterestService;

@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/interest")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
public class InterestController {

    private final InterestService interestService;

    //  [1] 관심 국가 목록 조회 (등록한 순서대로)
    //  예) GET /api/interest?memberId=dcdc8696-243a-43d1-ad68-138b293638f0
    @GetMapping ("")
    public List<InterestDto> interestRead(@RequestParam (name = "memberId") String memberId){

        return interestService.interestRead(memberId);
    }

    //  [2] 관심 국가 추가 (3개가 차 있으면 가장 먼저 등록한 국가를 빼고 추가)
    //  예) POST /api/interest   body { "memberId": "dcdc8696-...", "countryId": 1233 }
    //  @CookieValue : 로그인할 때 받은 출입증(AccessToken) 쿠키 값 → 본인인지 확인할 때 사용
    @PostMapping ("")
    public boolean interestWrite(
            @RequestBody InterestDto interestDto,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        return interestService.interestWrite(interestDto, accessToken);
    }

    //  [3] 관심 국가 삭제
    //  예) DELETE /api/interest/7
    @DeleteMapping ("/{interestId}")
    public boolean interestDelete(
            @PathVariable ("interestId") Integer interestId,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        return interestService.interestDelete(interestId, accessToken);
    }

}
