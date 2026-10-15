package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.ActionDto;
import main_project.model.dto.AuditPageDto;
import main_project.service.AuditService;
import main_project.service.AuthorizationService;

//  감사 로그 API — 관리자만 사용할 수 있음
//  (감사 로그에는 회원 이메일 · 접속 IP 가 들어 있어서 , 관리자가 아니면 조회하지 못하게 막음)
@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/admin")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
public class AuditController {

    private final AuditService auditService;

    //  관리자 확인은 사용자 권한 관리와 같은 메소드를 사용 (AuthorizationService.getLoginAdminId)
    private final AuthorizationService authorizationService;


//  감사 로그 목록 조회 — 한 페이지(50건)씩 (필터 조건은 선택)
//  예) /api/admin/audits?page=0&user=kim&memberType=비회원&actionId=3&ip=203.0&result=false&period=7days
//  - memberType : 회원 유형 (관리자 / 수출입기업 / 물류운송업체 / 비회원) , 안 보내면 전체
//  - actionId   : 작업 유형 번호 (필터 줄의 작업 유형 선택칸 — 목록은 아래 /api/admin/actions) , 안 보내면 전체
//  - ip         : IP 주소 검색어 , 안 보내면 전체
//  - page   : 몇 번째 페이지 (0 부터 — 0 = 1페이지) , 안 보내면 0
//             defaultValue : 값이 안 오면 쓸 기본값 (springweb day039 RestController2 의 count 와 같은 방식)
//  - period : 기간 (today / 7days / 30days / 6months / 1year) , 안 보내면 전체 기간
//  - 돌려주는 것 : 이번 페이지 로그 + 전체 건수 · 전체 페이지 수 (AuditPageDto) , 관리자가 아니면 null
//  @CookieValue : 로그인할 때 받은 출입증(AccessToken) 쿠키 (없으면 null)
    @GetMapping ("/audits")
    public AuditPageDto findAll(
            @RequestParam (name = "user", required = false) String user,
            @RequestParam (name = "memberType", required = false) String memberType,
            @RequestParam (name = "actionId", required = false) Integer actionId,
            @RequestParam (name = "ip", required = false) String ip,
            @RequestParam (name = "result", required = false) Boolean result,
            @RequestParam (name = "period", required = false) String period,
            @RequestParam (name = "page", required = false, defaultValue = "0") int page,
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 관리자인지 확인 (아니면 로그를 주지 않음)
        String adminId = authorizationService.getLoginAdminId(accessToken);
        if (adminId == null) { return null; }

        // 2. 관리자면 감사 로그 중 그 페이지만 조회
        return auditService.findAll(user, memberType, actionId, ip, result, period, page);
    }


//  작업 유형 목록 — 감사 로그 필터 줄의 "작업 유형" 선택칸에 넣을 21가지 (action 테이블)
//  예) GET /api/admin/actions → [ { "actionId": 1, "actionType": "엑셀 다운로드" } , ... ]
//  - 관리자만 (감사 로그와 같은 확인) , 관리자가 아니면 null
//  - 감사 로그 기록 대상(AuditTargets)에는 넣지 않음 : 화면을 열 때마다 자동으로 부르는 목록이라 기록할 필요가 없음
    @GetMapping ("/actions")
    public List<ActionDto> findActions(
            @CookieValue (name = "AccessToken", required = false) String accessToken){

        // 1. 관리자인지 확인
        String adminId = authorizationService.getLoginAdminId(accessToken);
        if (adminId == null) { return null; }

        // 2. 관리자면 작업 유형 목록
        return auditService.findActions();
    }

}
