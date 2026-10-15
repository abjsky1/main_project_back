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
@Table(name = "signup")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupEntity extends BaseTime {

    @Id
    @Column(name = "signup_id")
    private String signupId;

    @Column(name = "signup_type", nullable = false, length = 20)
    private String signupType;
}
