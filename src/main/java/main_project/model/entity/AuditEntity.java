package main_project.model.entity;

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
@Table(name = "audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Integer auditId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    @ManyToOne(fetch = FetchType.LAZY , optional = false)
    @JoinColumn(name = "action_id" , nullable = false)
    private ActionEntity actionEntity;

    @Column(name = "action_detail", nullable = false, length = 300)
    private String actionDetail;

    @Column(name = "fip_address", nullable = false, length = 45)
    private String fipAddress;

    @Column(name = "action_result", nullable = false)
    private Integer actionResult;
}
