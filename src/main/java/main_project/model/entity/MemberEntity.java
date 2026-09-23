package main_project.model.entity;

import java.time.LocalDateTime;
import java.util.PrimitiveIterator;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
public class MemberEntity {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer member_id;

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn(name = "signup_type_no", nullable = false)
    private Member_typeEntity member_typeEntity;

    @Column (name = "user_email", nullable = false, unique = true, length = 100)
    private String user_email;

    @Column ( name = "user_password" , nullable = false , length = 255 )
    private String user_password;

    @Column ( name = "company_name" , nullable = false , length = 100 )
    private String company_name;

    @Column ( name = "manager_name" , nullable = false , length = 50 )
    private String manager_name;

    @Column ( name = "department_name" , length = 50 )
    private String department_name;

    @Column ( name = "business_reg_no" , nullable = false , unique = true )
    private String business_reg_no;

    @Column ( name = "user_phone" , nullable = false , length = 20 )
    private String user_phone;

    @Column ( name = "company_address" , nullable = false , length = 300 )
    private String company_address;

    @CreationTimestamp 
    @Column ( name = "created_at" , nullable = false , updatable = false)
    private LocalDateTime created_at;

    @UpdateTimestamp 
    @Column ( name = "update_at")
    private LocalDateTime update_at;

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn(name = "role_id", nullable = false)
    private Member_roleEntity member_roleEntity;

    @Builder.Default
    @Column ( name = "status" , nullable = false )
    private Integer status = 1 ;


}
