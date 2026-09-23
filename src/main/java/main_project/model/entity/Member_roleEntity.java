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
@Table (name = "member_role")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class Member_roleEntity {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer role_id;

    @Column (length = 20 , nullable = false, unique = true)
    private String role_name;
    
    @Column  ( length = 100 , nullable = false)
    private String role_description;
    
}
