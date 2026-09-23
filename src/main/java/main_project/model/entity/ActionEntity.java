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
@Table (name = "action")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class ActionEntity {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer action_id;

    @Column (length = 50 , nullable = false)
    private String action_type;
    
}
