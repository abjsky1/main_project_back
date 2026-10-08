package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuditDto;
import main_project.service.AuditService;
import main_project.service.AuthorizationService;

//  감사 로그 API — 관리자만 사용할 수 있음
//  (감사 로그에는 회원 이메일 · 접속 IP 가 들어 있어서 , 관리자가 아니면 조회하지 못하게 막음)
@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/audit")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
public class AuditController {

    private final AuditService auditService;

    //  관리자 확인은 사용자 권한 관리와 같은 메소드를 사용 (AuthorizationService.getLoginAdminId)
    private final AuthorizationService authorizationService;


//  감사 로그 목록 조회 (필터 조건은 선택)
//  예) /api/audit?user=비회원&action=로그인&result=false
//  @CookieValue : 로그인할 때 받은 출입증(AccessToken) 쿠키 (없으면 null)
    @GetMapping ("")
    public List<AuditDto> findAll(
            @RequestParam (name = "user", required = false) String user,
            @RequestParam (name = "action", required = false) String action,
            @RequestParam (name = "result", required = false) Boolean result,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 관리자인지 확인 (아니면 로그를 주지 않음)
        String adminId = authorizationService.getLoginAdminId(accessToken);
        if (adminId == null) { return null; }

        // 2. 관리자면 감사 로그 조회
        return auditService.findAll(user, action, result);
    }

}
