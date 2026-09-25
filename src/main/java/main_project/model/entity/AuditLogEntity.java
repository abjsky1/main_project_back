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

@Entity 
@Table ( name = "auditLog")
@Builder 
@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class AuditLogEntity extends BaseTime {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer auditId;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "memberId" , nullable = false )
    private MemberEntity memberEntity;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "actionId" , nullable = false )
    private ActionEntity actionEntity;

    @Column ( name = "actionDetail" , length = 300, nullable = false)
    private String actionDetail;

    @Column ( name = "fipAddress" , length = 45, nullable = false)
    private String fip_Address;

    @Column ( name = "actionResult" , nullable = false)
    private Integer actionResult;

}
