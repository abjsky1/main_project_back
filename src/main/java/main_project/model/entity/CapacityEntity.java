package main_project.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "capacity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capacity_id")
    private Integer capacityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity routeEntity;

    @Column(name = "available_date", nullable = false, length = 50)
    private String availableDate;

    @Column(name = "max_capacity", nullable = false, length = 50)
    private String maxCapacity;

    @Builder.Default
    @Column(name = "reserved_capacity", length = 50)
    private String reservedCapacity = "0";

    @Column(name = "available_capacity", nullable = false, length = 50)
    private String availableCapacity;
}
