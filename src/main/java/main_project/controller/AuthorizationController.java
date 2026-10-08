package main_project.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationCountDto;
import main_project.service.AuthorizationService;

//  사용자 권한 관리 API — 관리자만 사용할 수 있음
//  - 화면(/admin)에서 막는 것만으로는 부족함 : 주소만 알면 Postman · curl 로 직접 호출할 수 있기 때문
//  - 그래서 메소드마다 맨 앞에서 로그인 쿠키(AccessToken)로 관리자인지 확인하고 , 아니면 바로 끝냄
//    (관리자가 아니면 조회는 null , 변경은 false — springweb day058 [3] 내 정보 조회 의 "if(token == null){return null;}" 과 같은 방식)
@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/authorization")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    //  사용자 권한 관리 사용자 목록 조회 (필터 조건은 선택)
    //  예) /api/authorization?roleName=ROLE_ADMIN&status=true
    //  @CookieValue : 로그인할 때 받은 출입증(AccessToken) 쿠키 (없으면 null)
    @GetMapping ("")
    public AuthorizationCountDto findAll(
            @RequestParam (name = "roleName", required = false) String roleName,
            @RequestParam (name = "status", required = false) Boolean status,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 관리자인지 확인 (아니면 목록을 주지 않음)
        String adminId = authorizationService.getLoginAdminId(accessToken);
        if (adminId == null) { return null; }

        // 2. 관리자면 목록 조회
        return authorizationService.findAll(roleName, status);
    }

    //  권한 변경 스위치 (관리자 ↔ 일반 사용자)
    @PutMapping ("/{memberId}/role")
    public boolean toggleRole(
            @PathVariable ("memberId") String memberId,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 관리자인지 확인
        String adminId = authorizationService.getLoginAdminId(accessToken);
        if (adminId == null) { return false; }

        // 2. 내 계정의 권한은 바꿀 수 없음 (실수로 관리자 권한을 잃어서 관리 화면에 못 들어오는 것 방지)
        if (adminId.equals(memberId)) { return false; }

        // 3. 권한 변경
        return authorizationService.toggleRole(memberId);
    }

    //  상태 변경 스위치 (활성 ↔ 비활성)
    @PutMapping ("/{memberId}/status")
    public boolean toggleStatus(
            @PathVariable ("memberId") String memberId,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 관리자인지 확인
        String adminId = authorizationService.getLoginAdminId(accessToken);
        if (adminId == null) { return false; }

        // 2. 내 계정은 비활성으로 바꿀 수 없음 (스스로 로그인을 막는 것 방지)
        if (adminId.equals(memberId)) { return false; }

        // 3. 상태 변경
        return authorizationService.toggleStatus(memberId);
    }

}
