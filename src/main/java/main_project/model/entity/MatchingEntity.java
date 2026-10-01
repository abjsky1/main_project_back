package main_project.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @JoinColumn(name = "cscore1_id", nullable = false)
    private Cscore1Entity cscore1Entity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lscore1_id", nullable = false)
    private Lscore1Entity lscore1Entity;

    @Builder.Default
    @Column(name = "route_score" , nullable = false)
    private Integer routeScore = 0;

    @Builder.Default
    @Column(name = "capacity_score" , nullable = false)
    private Integer capacityScore = 0;

    @Builder.Default
    @Column(name = "item_score" , nullable = false)
    private Integer itemScore = 0;

    @Builder.Default
    @Column(name = "schedule_score" , nullable = false)
    private Integer scheduleScore = 0;

    @Builder.Default
    @Column(name = "experience_score" , nullable = false)
    private Integer experienceScore = 0;

    @Builder.Default
    @Column(name = "total_score" , nullable = false)
    private Integer totalScore = 0;

    @Builder.Default
    @Column(name = "match_status", nullable = false)
    private Boolean matchStatus = false;

    @Builder.Default
    @Column(name = "recommend_reason", length = 300 , nullable = false)
    private String recommendReason = "-";

    @Builder.Default
    @Column(name = "warning_message", length = 300 , nullable = false)
    private String warningMessage = "-";
    
}