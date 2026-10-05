package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuditDto;
import main_project.service.AuditService;

@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/audit")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
public class AuditController {

    private final AuditService auditService;


//  감사 로그 목록 조회 (필터 조건은 선택)
//  예) /api/audit?user=비회원&action=로그인&result=false
    @GetMapping ("")
    public List<AuditDto> findAll(
            @RequestParam (name = "user", required = false) String user,
            @RequestParam (name = "action", required = false) String action,
            @RequestParam (name = "result", required = false) Boolean result){

        return auditService.findAll(user, action, result);
    }

}
