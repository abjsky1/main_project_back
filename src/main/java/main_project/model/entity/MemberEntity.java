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
@Table (name = "member")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class MemberEntity extends BaseTime {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer memberId;

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn(name = "signupTypeNo", nullable = false)
    private MemberTypeEntity memberTypeEntity;

    @Column (name = "userEmail", nullable = false, unique = true, length = 100)
    private String userEmail;

    @Column ( name = "userPassword" , nullable = false , length = 255 )
    private String userPassword;

    @Column ( name = "companyName" , nullable = false , length = 100 )
    private String companyName;

    @Column ( name = "managerName" , nullable = false , length = 50 )
    private String managerName;

    @Column ( name = "departmentName" , length = 50 )
    private String departmentName;

    @Column ( name = "businessRegNo" , nullable = false , unique = true )
    private String businessRegNo;

    @Column ( name = "userPhone" , nullable = false , length = 20 )
    private String userPhone;

    @Column ( name = "companyAddress" , nullable = false , length = 300 )
    private String companyAddress;

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn(name = "roleId", nullable = false)
    private MemberRoleEntity memberRoleEntity;

    @Builder.Default
    @Column(nullable = false, updatable = false )
    private Integer status = 1 ;


}
