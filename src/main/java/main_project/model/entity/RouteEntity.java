package main_project.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Integer routeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "transport_type", nullable = false, length = 50)
    private String transportType;

    @Column(name = "departure", nullable = false, length = 100)
    private String departure;

    @Column(name = "arrival", nullable = false, length = 100)
    private String arrival;

    @Builder.Default
    @Column(name = "regular_route")
    private Boolean regularRoute = true;

    @Builder.Default
    @Column(name = "direct_route")
    private Boolean directRoute = true;

    @Column(name = "average_transit_days", nullable = false)
    private Integer averageTransitDays;
}
