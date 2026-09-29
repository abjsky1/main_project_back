package main_project.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "capability")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capability_id")
    private Integer capabilityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    @Builder.Default
    @Column(name = "general_container", nullable = false)
    private Boolean generalContainer = true;

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
    private Boolean matchingAgree = true;
    
}
