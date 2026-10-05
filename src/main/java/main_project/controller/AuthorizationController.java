package main_project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationCountDto;
import main_project.service.AuthorizationService;

@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/authorization")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    //  사용자 권한 관리 사용자 목록 조회 (필터 조건은 선택)
    //  예) /api/authorization?roleName=ROLE_ADMIN&status=true
    @GetMapping ("")
    public AuthorizationCountDto findAll(
            @RequestParam (name = "roleName", required = false) String roleName,
            @RequestParam (name = "status", required = false) Boolean status){

        return authorizationService.findAll(roleName, status);
    }

    //  권한 변경 스위치 (관리자 ↔ 일반 사용자)
    @PutMapping ("/{memberId}/role")
    public boolean toggleRole(@PathVariable ("memberId") String memberId){

        return authorizationService.toggleRole(memberId);
    }

    //  상태 변경 스위치 (활성 ↔ 비활성)
    @PutMapping ("/{memberId}/status")
    public boolean toggleStatus(@PathVariable ("memberId") String memberId){

        return authorizationService.toggleStatus(memberId);
    }

}
