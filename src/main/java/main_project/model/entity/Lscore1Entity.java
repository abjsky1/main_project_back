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
@Table(name = "lscore1")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lscore1Entity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lscore1_id")
    private Integer lscore1Id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "hs_code", nullable = false, length = 15)
    private String hsCode;

    @Column(name = "trade_type", nullable = false, length = 20)
    private String tradeType;

    @Column(name = "transport_type", nullable = false, length = 50)
    private String transportType;

    @Column(name = "departure", nullable = false, length = 100)
    private String departure;

    @Column(name = "arrival", nullable = false, length = 100)
    private String arrival;

    @Builder.Default
    @Column(name = "matching_agree")
    private Boolean matchingAgree = false;

    @Builder.Default
    @Column(name = "experience_count")
    private Integer experienceCount = 0;

    @Builder.Default
    @Column(name = "regular_route")
    private Boolean regularRoute = true;

    @Builder.Default
    @Column(name = "direct_route")
    private Boolean directRoute = true;
}