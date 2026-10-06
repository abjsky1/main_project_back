package main_project.audit;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;

//  =====================================================================
//  감사 로그 도우미 ② : 화면 "대상" 칸에 들어갈 문구 만들기
//
//  등록표(AuditTargets)의 기본 문구에 , 작업 유형마다 필요한 정보를 덧붙임.
//
//    작업 유형                          | 결과 예시
//    -----------------------------------+-----------------------------------------------
//    로그인 / 로그인 실패               | 로그인 (입력 이메일: admin@naver.com)
//    회원 가입                          | 신규 회원 가입 (new@a.com)
//    매칭 조건 삭제 / 실행 / 승인 / 수락 | 화주 매칭 조건 삭제 (#12)
//    매칭 거절 / 반려                   | 매칭 반려 (#5) - 사유: 일정 불일치   (사유가 있을 때만)
//    사용자 권한 변경                   | 관리자 지정 (대상: 김승영 · ybtex@test.com)
//    사용자 상태 변경                   | 비활성화 (대상: 강테스트 · kang@test.com)
//    그 외 (데이터 조회 등)             | 등록표 문구 그대로
//
//  새 작업 유형에 정보를 붙이고 싶으면 makeDetail() 에 else if 를 하나 추가하면 됨.
//  =====================================================================
@Component
@RequiredArgsConstructor
public class AuditDetailMaker {

//  매칭 거절 / 반려 요청에서 사유가 담기는 칸 이름 (팀원 API 와 이름을 맞춰야 함)
    private static final String REJECT_REASON_FIELD = "rejectReason";

//  role 테이블에서 관리자(ROLE_ADMIN) 번호
    private static final int ADMIN_ROLE_ID = 2;

    private final AuditRequestReader requestReader;    // 요청에서 이메일 , 번호 , 사유 꺼내기
    private final MemberRepository memberRepository;   // 권한/상태 변경 대상 회원 찾기


//  "대상" 문구 만들기
//  target     : 등록표 한 줄 (기본 문구가 들어 있음)
//  actionType : 이번에 기록할 작업 유형 (성공/실패에 따라 고른 것)
//  success    : 성공 여부
//  request    : 지금 요청 (주소 번호 , 쿼리 파라미터)
//  args       : 컨트롤러가 받은 값들 (요청 body DTO 등)
    public String makeDetail(AuditTarget target, String actionType, boolean success, HttpServletRequest request, Object[] args){

        String detail = target.getDetail();

    //  로그인 / 로그인 실패 : 입력한 이메일 (미가입 이메일로 시도해도 그대로 남김)
        if (actionType.equals("로그인") || actionType.equals("로그인 실패")) {
            return detail + " (입력 이메일: " + valueOrDash(requestReader.readField(args, "userEmail")) + ")";
        }

    //  회원 가입 : 가입한 이메일
        else if (actionType.equals("회원 가입")) {
            return detail + " (" + valueOrDash(requestReader.readField(args, "userEmail")) + ")";
        }

    //  매칭 조건 삭제 , 매칭 실행 / 승인 / 수락 : 주소의 번호
        else if (actionType.equals("매칭 조건 삭제") || actionType.equals("매칭 실행")
              || actionType.equals("매칭 승인 (알림 발송)") || actionType.equals("매칭 수락")) {
            return detail + numberText(request);
        }

    //  매칭 거절 / 반려 : 주소의 번호 + 거절 사유
        else if (actionType.equals("매칭 거절") || actionType.equals("매칭 반려")) {
            return detail + numberText(request) + rejectReasonText(request, args);
        }

    //  권한 변경 : 스위치(관리자 ↔ 일반)라서 "바뀐 결과"를 보고 문구 결정
        else if (actionType.equals("사용자 권한 변경")) {
            return roleChangeText(detail, success, requestReader.pathValue(request, "memberId"));
        }

    //  상태 변경 : 스위치(활성 ↔ 비활성)라서 "바뀐 결과"를 보고 문구 결정
        else if (actionType.equals("사용자 상태 변경")) {
            return statusChangeText(detail, success, requestReader.pathValue(request, "memberId"));
        }

    //  그 외 (데이터 조회 등) : 등록표 문구 그대로
        else {
            return detail;
        }
    }


//  주소의 번호 → " (#12)"   (번호가 없으면 빈 글자)
    private String numberText(HttpServletRequest request){
        String number = requestReader.firstPathValue(request);
        if (number == null) {
            return "";
        }
        return " (#" + number + ")";
    }


//  거절 / 반려 사유 → " - 사유: 일정 불일치"   (사유가 없으면 빈 글자)
//  ① 주소 뒤 ?rejectReason=...  →  ② 요청 body 의 rejectReason 칸  순서로 찾음
    private String rejectReasonText(HttpServletRequest request, Object[] args){
        String reason = requestReader.requestParam(request, REJECT_REASON_FIELD);

        if (reason == null || reason.isBlank()) {
            Object value = requestReader.readField(args, REJECT_REASON_FIELD);
            if (value != null) {
                reason = value.toString();
            }
        }

        if (reason == null || reason.isBlank()) {
            return "";
        }
        return " - 사유: " + reason;
    }


//  권한 변경 문구 : "관리자 지정 (대상: ...)" / "권한 해제 (대상: ...)"
//  이 코드는 컨트롤러가 끝난 "뒤"에 실행되므로 , 회원을 다시 찾으면 이미 바뀐 값이 보임
    private String roleChangeText(String detail, boolean success, String memberId){
        MemberEntity memberEntity = findMember(memberId);

    //  실패했거나 회원을 못 찾으면 : 기본 문구 + 대상
        if (!success || memberEntity == null) {
            return detail + targetText(memberEntity, memberId);
        }

    //  getRoleId() 는 role 정보를 추가로 조회하지 않고도 번호를 읽을 수 있음
        boolean isAdmin = memberEntity.getRoleEntity().getRoleId() == ADMIN_ROLE_ID;
        if (isAdmin) {
            return "관리자 지정" + targetText(memberEntity, memberId);
        }
        return "권한 해제" + targetText(memberEntity, memberId);
    }


//  상태 변경 문구 : "활성화 (대상: ...)" / "비활성화 (대상: ...)"
    private String statusChangeText(String detail, boolean success, String memberId){
        MemberEntity memberEntity = findMember(memberId);

        if (!success || memberEntity == null) {
            return detail + targetText(memberEntity, memberId);
        }

        if (Boolean.TRUE.equals(memberEntity.getStatus())) {
            return "활성화" + targetText(memberEntity, memberId);
        }
        return "비활성화" + targetText(memberEntity, memberId);
    }


//  " (대상: 이름 · 이메일)"   (회원을 못 찾으면 번호만)
    private String targetText(MemberEntity memberEntity, String memberId){
        if (memberEntity == null) {
            return " (대상: " + valueOrDash(memberId) + ")";
        }
        return " (대상: " + memberEntity.getManagerName() + " · " + memberEntity.getUserEmail() + ")";
    }

//  회원 번호로 회원 찾기 (번호가 없거나 없는 회원이면 null)
    private MemberEntity findMember(String memberId){
        if (memberId == null) {
            return null;
        }
        return memberRepository.findById(memberId).orElse(null);
    }

//  값이 없으면 "-"
    private String valueOrDash(Object value){
        if (value == null) {
            return "-";
        }
        return value.toString();
    }

}
