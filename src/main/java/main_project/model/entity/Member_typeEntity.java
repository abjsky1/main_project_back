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
@Table (name="member_type")
@Data 
@Builder 
@AllArgsConstructor 
@NoArgsConstructor 
public class Member_typeEntity {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer signup_type_no;

    @Column(length = 20, nullable = false)
    private String signup_type;
}
