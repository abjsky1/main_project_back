package main_project.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "action")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Integer actionId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;
}
