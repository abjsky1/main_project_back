package main_project.audit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

//  =====================================================================
//  감사 로그 등록표
//
//  - 여기에 적힌 API 만 AuditAspect 가 감사 로그(audit 테이블)로 기록함
//  - 팀원 API 가 새로 생기면 아래 생성자에 add(...) 한 줄만 추가하면 끝 (팀원 코드는 수정 X)
//
//  add( HTTP메소드 , 주소 패턴 , 작업 유형 , [실패 시 작업 유형] , 대상 문구 )
//   - HTTP메소드        : GET(조회) / POST(등록·실행) / PUT(수정) / DELETE(삭제)
//   - 주소 패턴         : 컨트롤러 @RequestMapping + @GetMapping 등을 합친 주소 그대로
//                         주소 안의 번호는 {이름} 으로 적음  예) /api/cscore/{cscore1Id}
//   - 작업 유형         : action 테이블의 action_type 과 글자까지 똑같이 (다르면 서버 시작 시 콘솔 경고)
//   - 실패 시 작업 유형 : 생략하면 성공/실패 모두 같은 작업 유형으로 기록
//   - 대상 문구         : 화면 "대상" 칸에 보이는 기본 문구
//                         (입력 이메일 , 번호 , 거절 사유 등은 AuditDetailMaker 가 뒤에 덧붙임)
//  =====================================================================
@Component   // 스프링이 서버를 켤 때 이 등록표를 하나 만들어서 AuditAspect 에 넣어 줌
public class AuditTargets {

//  등록표 보관함 (사전처럼 "찾는 말 → 내용")
//  찾는 말(key) : "HTTP메소드 주소패턴"   예) "DELETE /api/cscore/{cscore1Id}"
//  내용(value)  : 그 API 의 기록 방법 (AuditTarget 한 줄)
    private final Map<String, AuditTarget> targets = new HashMap<>();


//  생성자 : 서버가 켜질 때 한 번 실행되면서 등록표를 채움
    public AuditTargets(){

    //  ---------- 로그인 / 회원 ----------
        add("POST",   "/api/login",  "로그인", "로그인 실패", "로그인");
        add("POST",   "/api/signup", "회원 가입",            "신규 회원 가입");

    //  ---------- 매칭 조건 (화주 = cscore , 물류 = lscore) ----------
        add("POST",   "/api/cscore",             "매칭 조건 등록", "화주 매칭 조건 등록");
        add("DELETE", "/api/cscore/{cscore1Id}", "매칭 조건 삭제", "화주 매칭 조건 삭제");
        add("POST",   "/api/lscore",             "매칭 조건 등록", "물류 매칭 조건 등록");
        add("DELETE", "/api/lscore/{lscore1Id}", "매칭 조건 삭제", "물류 매칭 조건 삭제");

    //  ---------- 매칭 ----------
        add("POST",   "/api/matching/run/{cscore1Id}",                "매칭 실행",            "매칭 실행");
        add("POST",   "/api/matching/approve/{matchingId}",           "매칭 승인 (알림 발송)", "매칭 승인");
        add("POST",   "/api/matching/reject/{matchingId}",            "매칭 반려",            "매칭 반려");
        add("POST",   "/api/matching/shipper/accept/{matchingId}",    "매칭 수락",            "화주 매칭 수락");
        add("POST",   "/api/matching/shipper/reject/{matchingId}",    "매칭 거절",            "화주 매칭 거절");
        add("POST",   "/api/matching/logistics/accept/{matchingId}",  "매칭 수락",            "물류 매칭 수락");
        add("POST",   "/api/matching/logistics/reject/{matchingId}",  "매칭 거절",            "물류 매칭 거절");
    //  (거절 / 반려 요청에 rejectReason 이 있으면 AuditDetailMaker 가 " - 사유: ..." 를 자동으로 붙임)

    //  ---------- 마이페이지 ----------
        add("PUT",    "/api/mypage/{memberId}", "기업 정보 수정", "회원 정보 수정 (이름·주소)");
        add("POST",   "/api/mypage/withdraw",   "회원 탈퇴",      "회원 탈퇴");
    //  (탈퇴에 성공하면 회원이 이미 삭제된 뒤라서 , 이 로그는 "탈퇴 회원" 공통 계정으로 기록됨 — AuditService.record)
    //  (관심 국가 추가·삭제 /api/interest 는 action 테이블에 맞는 작업 유형이 없어서 등록 안 함)

    //  ---------- 사용자 권한 관리 (시스템 관리) ----------
        add("PUT",    "/api/authorization/{memberId}/role",   "사용자 권한 변경", "사용자 권한 변경");
        add("PUT",    "/api/authorization/{memberId}/status", "사용자 상태 변경", "사용자 상태 변경");

    //  ---------- 데이터 조회 ----------
        add("GET", "/api/cscore",                     "데이터 조회", "화주 매칭 조건 목록 조회");
        add("GET", "/api/lscore",                     "데이터 조회", "물류 매칭 조건 목록 조회");
        add("GET", "/api/matching",                   "데이터 조회", "매칭 결과 목록 조회");
        add("GET", "/api/matching/member/{memberId}", "데이터 조회", "내 매칭 알림 조회");
        add("GET", "/api/cumulative/trade",           "데이터 조회", "누적 무역 현황 조회");
        add("GET", "/api/exchangerate/nation",        "데이터 조회", "국가별 환율 조회");
        add("GET", "/api/today/exchangerate",         "데이터 조회", "당일 환율 조회");
        add("GET", "/api/exchange/month",             "데이터 조회", "월별 환율 조회");
        add("GET", "/api/authorization",              "데이터 조회", "사용자 권한 관리 목록 조회");

    //  ※ 일부러 등록 안 한 것
    //    - /api/cscore/{id}/cscore2 처럼 표의 줄마다 반복 호출되는 상세 조회 (로그가 너무 많아짐)
    //    - 감사 로그 조회(/api/audit) 자체 (로그를 볼 때마다 로그가 쌓이는 것 방지)
    }


//  등록하기 ① 성공 / 실패 작업 유형이 같을 때 (대부분)
    private void add(String httpMethod, String urlPattern, String actionType, String detail){
        add(httpMethod, urlPattern, actionType, actionType, detail);
    }

//  등록하기 ② 성공 / 실패 작업 유형이 다를 때 (예: 로그인 / 로그인 실패)
    private void add(String httpMethod, String urlPattern, String successActionType, String failActionType, String detail){
        String key = httpMethod + " " + urlPattern;
        targets.put(key, new AuditTarget(successActionType, failActionType, detail));
    }


//  지금 요청이 등록표에 있는지 찾기
//  있으면 그 줄(AuditTarget) , 없으면 null → AuditAspect 는 기록하지 않고 원래 메소드만 실행
    public AuditTarget find(String httpMethod, String urlPattern){
        if (urlPattern == null) {
            return null;
        }
        String key = httpMethod + " " + urlPattern;
        return targets.get(key);
    }


//  등록표에 쓰인 작업 유형 이름 전체
//  (서버 시작 시 AuditService.checkAuditTargets() 가 action 테이블에 다 있는지 비교할 때 사용)
    public Set<String> actionTypes(){
        Set<String> result = new HashSet<>();
        for (AuditTarget target : targets.values()) {
            result.add(target.getSuccessActionType());
            result.add(target.getFailActionType());
        }
        return result;
    }

}
