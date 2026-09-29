package main_project.model.entity;

import java.time.LocalDate;

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

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Column(name = "max_capacity", nullable = false)
    private Double maxCapacity;

    @Builder.Default
    @Column(name = "reserved_capacity", nullable = false)
    private Double reservedCapacity = 0.0;

    @Column(name = "available_capacity", nullable = false)
    private Double availableCapacity;
}