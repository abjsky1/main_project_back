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

@Entity
@Table(name = "lscore3")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lscore3Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lscore3_id")
    private Integer lscore3Id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lscore1_id", nullable = false)
    private Lscore1Entity lscore1Entity;

    @Builder.Default
    @Column(name = "general_container")
    private Boolean generalContainer = true;

    @Builder.Default
    @Column(name = "refrigerated")
    private Boolean refrigerated = false;

    @Builder.Default
    @Column(name = "dangerous")
    private Boolean dangerous = false;

    @Builder.Default
    @Column(name = "heavy_cargo")
    private Boolean heavyCargo = false;

    @Builder.Default
    @Column(name = "special_cargo")
    private Boolean specialCargo = false;
}