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
@Table(name = "cscore1")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cscore1Entity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cscore1_id")
    private Integer cscore1Id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "hs_code", nullable = false, length = 15)
    private String hsCode;

    @Column(name = "trade_type", nullable = false)
    private Boolean tradeType;

    @Column(name = "transport_type", nullable = false)
    private Boolean transportType;

    @Column(name = "departure", nullable = false)
    private Integer departure;

    @Column(name = "arrival", nullable = false)
    private Integer arrival;

    @Builder.Default
    @Column(name = "matching_agree" , nullable = false)
    private Boolean matchingAgree = false;
}