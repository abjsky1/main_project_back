package main_project.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "hscode")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HscodeEntity {

    @Id
    @Column(name = "hscode_id", length = 10)
    private String hscodeId;

    @Column(name = "hscode_name", length = 500, nullable = false)
    private String hscodeName;

}