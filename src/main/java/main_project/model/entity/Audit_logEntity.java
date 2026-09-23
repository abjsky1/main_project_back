package main_project.model.entity;

import java.time.LocalDateTime;

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
@Table 
@Builder 
@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class Audit_logEntity {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer audit_id;

    @Column (nullable = false)
    private LocalDateTime created_at = LocalDateTime.now();

    @Column (nullable = false)
    private Integer member_id;

    @Column (nullable = false)
    private Integer action_id;

    @Column (length = 300, nullable = false)
    private String action_datail;

    @Column (length = 45, nullable = false)
    private String fip_address;

    @Column (nullable = false)
    private Integer action_result;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "member_id")
    private MemberEntity memberEntity;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "action_id")
    private ActionEntity actionEntity;

}
