package main_project.model.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@EntityListeners(AuditingEntityListener.class)
public class AuditEntity {

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
    private Boolean actionResult;

    //  생성 시간만 보관 (저장될 때 JPA 가 자동으로 현재 시간을 넣어줌)
    //  updatable = false : 한 번 저장된 뒤에는 UPDATE 문에 포함되지 않음 → 기록 시간이 바뀔 일 없음
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)   
    private LocalDateTime createdAt;
    
}
