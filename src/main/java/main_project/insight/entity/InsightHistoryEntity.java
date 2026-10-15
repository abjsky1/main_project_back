package main_project.insight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(
        name = "insight_history",
        indexes = {
                @Index( // 기록 조회를 빠르게 하기 위한 DB 인덱스
                    name = "idx_insight_history_member_created",
                    columnList = "member_id,created_at"
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insight_id")
    private Integer insightId;

    @Column(name = "member_id", nullable = false, length = 100)
    private String memberId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "item_name", nullable = false, length = 2000)
    private String itemName;

    @Column(name = "hs_code", nullable = false, length = 10)
    private String hsCode;

    @Column(name = "direction", nullable = false, length = 10)
    private String direction;

    @Column(name = "start_month", nullable = false, length = 7)
    private String startMonth;

    @Column(name = "end_month", nullable = false, length = 7)
    private String endMonth;

    @Lob // 큰 데이터를 저장
    @ToString.Exclude
    @Column(name = "condition_json", nullable = false, columnDefinition = "LONGTEXT")
    private String conditionJson;

    @Lob
    @ToString.Exclude
    @Column(name = "result_json", nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

    @Lob
    @ToString.Exclude
    @Column(name = "metadata_json", nullable = false, columnDefinition = "LONGTEXT")
    private String metadataJson;
}
