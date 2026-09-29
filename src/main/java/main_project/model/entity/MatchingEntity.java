package main_project.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "matching")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_id")
    private Integer matchingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private RequestEntity requestEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_member_id", nullable = false)
    private MemberEntity companyMemberEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logistics_member_id", nullable = false)
    private MemberEntity logisticsMemberEntity;

    @Builder.Default
    @Column(name = "route_score", nullable = false)
    private Integer routeScore = 0;

    @Builder.Default
    @Column(name = "capacity_score", nullable = false)
    private Integer capacityScore = 0;

    @Builder.Default
    @Column(name = "item_score", nullable = false)
    private Integer itemScore = 0;

    @Builder.Default
    @Column(name = "schedule_score", nullable = false)
    private Integer scheduleScore = 0;

    @Builder.Default
    @Column(name = "transport_score", nullable = false)
    private Integer transportScore = 0;

    @Builder.Default
    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(name = "match_status", nullable = false)
    private Integer matchStatus;

    @Column(name = "recommend_reason", length = 300)
    private String recommendReason;

    @Column(name = "warning_message", length = 300)
    private String warningMessage;
}
