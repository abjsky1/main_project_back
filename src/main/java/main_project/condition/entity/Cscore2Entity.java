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
@Table(name = "cscore2")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cscore2Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cscore2_id")
    private Integer cscore2Id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cscore1_id", nullable = false)
    private Cscore1Entity cscore1Entity;

    @Column(name = "request_weight", nullable = false)
    private Double requestWeight;

    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;
}