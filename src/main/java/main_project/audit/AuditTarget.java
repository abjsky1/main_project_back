package main_project.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;

//  =====================================================================
//  감사 로그 등록표(AuditTargets)의 "한 줄"
//
//  예) POST /api/login 한 줄
//      - 성공하면 작업 유형 "로그인"
//      - 실패하면 작업 유형 "로그인 실패"
//      - 화면 "대상" 칸의 기본 문구 "로그인"
//
//  @Getter            : getSuccessActionType() , getFailActionType() , getDetail() 을 Lombok 이 자동으로 만들어 줌
//  @AllArgsConstructor: new AuditTarget(성공 작업 유형 , 실패 작업 유형 , 기본 문구) 생성자를 자동으로 만들어 줌
//  =====================================================================
@Getter
@AllArgsConstructor
public class AuditTarget {

//  성공했을 때 작업 유형 (action 테이블의 action_type 과 글자까지 같아야 함)
    private String successActionType;

//  실패했을 때 작업 유형 (대부분 성공과 같고 , 로그인만 "로그인 실패" 로 다름)
    private String failActionType;

//  화면 "대상" 칸에 들어갈 기본 문구 (이메일 , 번호 같은 추가 정보는 AuditDetailMaker 가 뒤에 붙임)
    private String detail;


//  성공 여부에 맞는 작업 유형 골라 주기
//  예) 로그인 성공 → "로그인" , 로그인 실패 → "로그인 실패"
    public String getActionType(boolean success){
        if (success) {
            return successActionType;
        }
        return failActionType;
    }

}
