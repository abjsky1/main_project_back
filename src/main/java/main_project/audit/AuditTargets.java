package main_project.audit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

//  =====================================================================
//  감사 로그 등록표
//  - 여기에 등록된 API 만 AuditAspect 가 감사 로그(audit 테이블)로 기록함
//  - 팀원 API 가 새로 생기면 아래 생성자에 add(...) 한 줄만 추가하면 끝 (팀원 코드는 수정 X)
//
//  add( HTTP메소드 , 주소 패턴 , 작업 유형 , [실패 시 작업 유형] , 대상 문구 )
//   - 주소 패턴   : 컨트롤러 @RequestMapping + @GetMapping 등을 합친 주소 그대로 ( {번호} 포함 )
//   - 작업 유형   : action 테이블의 action_type 과 글자까지 똑같이 (다르면 서버 시작 시 콘솔 경고)
//   - 실패 시 작업 유형 : 생략하면 성공/실패 모두 같은 작업 유형으로 기록
//   - 대상 문구   : 화면 "대상" 칸에 보이는 자유 문구 (입력 이메일, 번호 등은 AuditAspect.makeDetail() 에서 덧붙임)
//  =====================================================================
@Component
public class AuditTargets {

//  등록된 API 1개의 기록 방법
    public record Target(String successAction, String failAction, String detail) {

    //  성공 여부에 맞는 작업 유형
        public String actionFor(boolean success){
            return success ? successAction : failAction;
        }
    }

//  key : "HTTP메소드 주소패턴"  예) "DELETE /api/cscore/{cscore1Id}"
    private final Map<String, Target> targets = new HashMap<>();

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
        add("POST",   "/api/matching/run/{cscore1Id}", "매칭 실행", "매칭 실행");
    //  TODO : 매칭 수락 / 거절 / 반려 API 가 생기면 주소에 맞게 추가 (거절 사유는 요청의 rejectReason 을 자동으로 붙임)
    //  add("POST", "/api/matching/{matchingId}/reject", "매칭 거절", "매칭 거절");

    //  ---------- 데이터 조회 ----------
        add("GET", "/api/cscore",                 "데이터 조회", "화주 매칭 조건 목록 조회");
        add("GET", "/api/lscore",                 "데이터 조회", "물류 매칭 조건 목록 조회");
        add("GET", "/api/matching",               "데이터 조회", "매칭 결과 목록 조회");
        add("GET", "/api/trade/year",             "데이터 조회", "무역 데이터 연도별 비교 조회");
        add("GET", "/api/cumulative/trade",       "데이터 조회", "누적 무역 현황 조회");
        add("GET", "/api/exchangerate/nation",    "데이터 조회", "국가별 환율 조회");
        add("GET", "/month",                      "데이터 조회", "월별 환율 조회");
        add("GET", "/api/hscode",                 "데이터 조회", "HS 코드 목록 조회");
        add("GET", "/macross/authorization/find", "데이터 조회", "사용자 권한 관리 목록 조회");

    //  ※ 일부러 등록 안 한 것 : /api/cscore/{id}/cscore2 처럼 표의 행마다 반복 호출되는 상세 조회 , 감사 로그 조회 자체
    }


//  성공 / 실패 작업 유형이 같을 때
    private void add(String httpMethod, String urlPattern, String actionType, String detail){
        add(httpMethod, urlPattern, actionType, actionType, detail);
    }

//  성공 / 실패 작업 유형이 다를 때 (예: 로그인 / 로그인 실패)
    private void add(String httpMethod, String urlPattern, String successAction, String failAction, String detail){
        targets.put(httpMethod + " " + urlPattern, new Target(successAction, failAction, detail));
    }

//  지금 요청이 등록표에 있는지 찾기 (없으면 null → 기록 안 함)
    public Target find(String httpMethod, String urlPattern){
        return targets.get(httpMethod + " " + urlPattern);
    }

//  등록표에 쓰인 작업 유형 이름 전체 (서버 시작 시 action 테이블과 비교용)
    public Set<String> actionTypes(){
        Set<String> result = new HashSet<>();
        for (Target target : targets.values()) {
            result.add(target.successAction());
            result.add(target.failAction());
        }
        return result;
    }

}
