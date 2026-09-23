package main_project.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

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
import lombok.Builder.Default;

@Entity 
@Table ( name = "audit_log" )
@Builder 
@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class Audit_logEntity extends BaseTime {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer audit_id;

    // @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    // private LocalDateTime created_at;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "member_id" , nullable = false )
    private MemberEntity memberEntity;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "action_id" , nullable = false )
    private ActionEntity actionEntity;

    @Column ( name = "action_detail" , length = 300, nullable = false)
    private String action_detail;

    @Column ( name = "fip_address" , length = 45, nullable = false)
    private String fip_address;

    @Column ( name = "action_result" , nullable = false)
    private Integer action_result;

}
