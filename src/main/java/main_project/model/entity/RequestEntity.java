package main_project.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

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

    @Column(name = "request_weight", nullable = false)
    private Double requestWeight;

    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;

    @Column(name = "cargo_type", nullable = false, length = 50)
    private String cargoType;

    @Builder.Default
    @Column(name = "refrigerated", nullable = false)
    private Boolean refrigerated = false;

    @Builder.Default
    @Column(name = "dangerous", nullable = false)
    private Boolean dangerous = false;

    @Builder.Default
    @Column(name = "heavy_cargo", nullable = false)
    private Boolean heavyCargo = false;

    @Builder.Default
    @Column(name = "special_cargo", nullable = false)
    private Boolean specialCargo = false;

    @Builder.Default
    @Column(name = "matching_agree", nullable = false)
    private Boolean matchingAgree = false;

}
