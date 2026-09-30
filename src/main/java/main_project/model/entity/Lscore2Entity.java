package main_project.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "lscore2")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lscore2Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lscore2_id")
    private Integer lscore2Id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lscore1_id", nullable = false)
    private Lscore1Entity lscore1Entity;

    @Column(name = "max_capacity", nullable = false)
    private Double maxCapacity;

    @Column(name = "available_capacity", nullable = false)
    private Double availableCapacity;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Column(name = "average_transit_days", nullable = false)
    private Integer averageTransitDays;
}
