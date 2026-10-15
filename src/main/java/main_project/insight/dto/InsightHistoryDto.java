package main_project.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import main_project.insight.entity.InsightHistoryEntity;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class InsightHistoryDto {

    private Integer insightId;

    private String memberId;
    private Instant createdAt;

    private String status;
    private String itemName;
    private String hsCode;
    private String direction;
    private String startMonth;
    private String endMonth;

    @ToString.Exclude
    private String conditionJson;

    @ToString.Exclude
    private String resultJson;

    @ToString.Exclude
    private String metadataJson;

    // DTO -> Entity
    // 새 기록의 번호는 DB에서 자동 생성한다.
    public InsightHistoryEntity toEntity() {

        return InsightHistoryEntity.builder()
                .memberId(memberId)
                .createdAt(createdAt)
                .status(status)
                .itemName(itemName)
                .hsCode(hsCode)
                .direction(direction)
                .startMonth(startMonth)
                .endMonth(endMonth)
                .conditionJson(conditionJson)
                .resultJson(resultJson)
                .metadataJson(metadataJson)
                .build();
    }

    // Entity -> DTO
    public static InsightHistoryDto from(
            InsightHistoryEntity entity
    ) {

        return InsightHistoryDto.builder()
                .insightId(entity.getInsightId())
                .memberId(entity.getMemberId())
                .createdAt(entity.getCreatedAt())
                .status(entity.getStatus())
                .itemName(entity.getItemName())
                .hsCode(entity.getHsCode())
                .direction(entity.getDirection())
                .startMonth(entity.getStartMonth())
                .endMonth(entity.getEndMonth())
                .conditionJson(entity.getConditionJson())
                .resultJson(entity.getResultJson())
                .metadataJson(entity.getMetadataJson())
                .build();
    }
}