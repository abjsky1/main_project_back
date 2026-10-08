package main_project.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MypageDto;
import main_project.service.MypageService;

@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/mypage")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
public class MypageController {

    private final MypageService mypageService;

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

}
