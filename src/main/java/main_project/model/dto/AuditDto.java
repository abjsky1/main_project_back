package main_project.model.dto;
 
import java.time.LocalDateTime;
 
import com.fasterxml.jackson.annotation.JsonFormat;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.AuditEntity;
 
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AuditDto {
 
//  감사로그 번호 (프론트에서 표의 행을 구분하는 key 로 사용)
    private Integer auditId;
 
//  일시 (화면 모양 그대로 "2025-12-31 09:31:45" 로 내려가게 포맷 지정)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
 
//  사용자 (member 테이블의 담당자 이름)
    private String managerName;
 
//  작업 유형 번호 (프론트에서 배지 색 구분용 , 한글 문자열보다 번호로 구분하는 게 안전)
    private Integer actionId;
 
//  작업 유형 (action 테이블의 이름 : 로그인, 엑셀 다운로드 ...)
    private String actionType;
 
//  대상 (상세 내용)
    private String actionDetail;
 
//  IP 주소
    private String fipAddress;
 
//  결과 (true : 성공 , false : 실패)
    private Boolean actionResult;
 
 
//  조회 전용 DTO 라서 toEntity() 는 만들지 않음.
//  (로그 저장 기능을 만들 때는 저장용 DTO 나 메소드를 따로 만드는 걸 추천)
 
    public static AuditDto from(AuditEntity auditEntity){
        return AuditDto.builder()
            .auditId(auditEntity.getAuditId())
            .createdAt(auditEntity.getCreatedAt())
            .managerName(auditEntity.getMemberEntity().getManagerName())
            .actionId(auditEntity.getActionEntity().getActionId())
            .actionType(auditEntity.getActionEntity().getActionType())
            .actionDetail(auditEntity.getActionDetail())
            .fipAddress(auditEntity.getFipAddress())
            .actionResult(auditEntity.getActionResult())
            .build();
    }
 
}