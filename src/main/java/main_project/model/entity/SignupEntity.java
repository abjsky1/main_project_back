package main_project.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "signup")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signup_id")
    private Integer signupId;

    @Column(name = "signup_type", nullable = false, length = 20)
    private String signupType;
}
