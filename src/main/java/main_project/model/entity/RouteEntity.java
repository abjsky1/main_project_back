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
@Table (name = "route")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class RouteEntity extends BaseTime {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "route_id")
    private Integer routeId;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "member_id", nullable = false)
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
    @Column(name = "regular_route" , nullable = false)
    private Boolean regularRoute = true;

    @Builder.Default
    @Column(name = "direct_route" , nullable = false)
    private Boolean directRoute = true;

    @Column(name = "average_transit_days", nullable = false)
    private Integer averageTransitDays;
}
