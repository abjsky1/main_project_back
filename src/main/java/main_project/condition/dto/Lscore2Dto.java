package main_project.model.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.Lscore2Entity;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Lscore2Dto {

    private Integer lscore2Id;

    private Integer lscore1Id;

    private Double availableCapacity;

    private LocalDate availableDate;

    private Integer averageTransitDays;


    // DTO -> Entity
    public Lscore2Entity toEntity(Lscore1Entity lscore1Entity) {

        return Lscore2Entity.builder()
                .lscore1Entity(lscore1Entity)
                .availableCapacity(availableCapacity)
                .availableDate(availableDate)
                .averageTransitDays(averageTransitDays)
                .build();
                
    }


    // Entity -> DTO
    public static Lscore2Dto from(Lscore2Entity entity) {

        return Lscore2Dto.builder()

                .lscore2Id(entity.getLscore2Id())
                .lscore1Id(entity.getLscore1Entity().getLscore1Id())
                .availableCapacity(entity.getAvailableCapacity())
                .availableDate(entity.getAvailableDate())
                .averageTransitDays(entity.getAverageTransitDays())
                .build();

    }

}