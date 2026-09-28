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
@Table(name = "member")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MemberEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Integer memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signup_type_no", nullable = false)
    private MemberTypeEntity memberTypeEntity;

    @Column(name = "user_email", nullable = false, unique = true, length = 100)
    private String userEmail;

    @Column(name = "user_password", nullable = false, length = 255)
    private String userPassword;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName;

    @Column(name = "department_name", length = 50)
    private String departmentName;

    @Column(name = "business_reg_no", nullable = false, unique = true, length = 12)
    private String businessRegNo;

    @Column(name = "user_phone", nullable = false, length = 20)
    private String userPhone;

    @Column(name = "company_address", nullable = false, length = 300)
    private String companyAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private MemberRoleEntity memberRoleEntity;

    @Builder.Default
    @Column(name = "status", nullable = false, updatable = false)
    private Integer status = 1;
    
}