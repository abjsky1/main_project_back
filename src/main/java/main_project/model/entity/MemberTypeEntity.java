package main_project.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_type")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberTypeEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signup_type_no")
    private Integer signupTypeNo;

    @Column(name = "signup_type", length = 20, nullable = false)
    private String signupType;
    
}
