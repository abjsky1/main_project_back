package main_project.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    @Column(name = "hs_code", nullable = false, length = 15)
    private String hsCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Builder.Default
    @Column(name = "experience_count" , nullable = false )
    private Integer experienceCount = 0;
}