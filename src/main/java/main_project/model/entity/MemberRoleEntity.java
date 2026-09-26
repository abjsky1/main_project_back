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
@Table (name = "memberRole")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class MemberRoleEntity extends BaseTime {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer roleId;

    @Column (length = 20 , nullable = false, unique = true)
    private String roleName;
    
    @Column  ( length = 100 , nullable = false)
    private String roleDescription;
    
}
