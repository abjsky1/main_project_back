package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.ActionEntity;
import main_project.model.entity.AuditEntity;
import main_project.model.entity.MemberEntity;

//  감사 로그 "저장용" DTO  (조회용은 AuditDto)
//  AuditAspect 가 요청 1건을 기록할 때 만들어서 AuditService.record() 로 넘김
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AuditSaveDto {

//  회원 번호 (비회원 / 못 찾으면 null → AuditService 에서 GUEST 로 저장)
    private String memberId;

//  작업 유형 이름 (action 테이블의 action_type 과 글자까지 같아야 함 : 로그인, 데이터 조회 ...)
    private String actionType;

//  대상 (화면의 "대상" 칸에 보이는 문구)
    private String actionDetail;

//  접속 IP
    private String fipAddress;

//  결과 (true : 성공 , false : 실패)
    private Boolean actionResult;


//  DTO → 엔티티 (회원 , 작업 유형은 서비스에서 찾아서 넘겨줌)
    public AuditEntity toEntity(MemberEntity memberEntity, ActionEntity actionEntity){
        return AuditEntity.builder()
            .memberEntity(memberEntity)
            .actionEntity(actionEntity)
            .actionDetail(actionDetail)
            .fipAddress(fipAddress)
            .actionResult(actionResult)
            .build();
    }

}
