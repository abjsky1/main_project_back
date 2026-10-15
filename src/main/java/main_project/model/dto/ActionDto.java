package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.ActionEntity;

//  작업 유형 1개 (서버 → 프론트) — 감사 로그 필터 줄의 "작업 유형" 선택칸 목록
//  예) { "actionId": 2, "actionType": "로그인" }
//  - 쓰이는 곳 : GET /api/admin/actions (AuditController)
//  - action 테이블의 행을 그대로 옮겨 담음 (작업 유형이 늘어나도 DB 에만 추가하면 선택칸에 자동으로 나옴)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ActionDto {

//  작업 유형 번호 (action_id) — 선택칸에서 고르면 이 번호로 감사 로그를 거름
    private Integer actionId;

//  작업 유형 이름 (action_type) — 선택칸에 보이는 글자
    private String actionType;

//  엔티티 → DTO
    public static ActionDto from(ActionEntity actionEntity){
        return ActionDto.builder()
            .actionId(actionEntity.getActionId())
            .actionType(actionEntity.getActionType())
            .build();
    }
}
