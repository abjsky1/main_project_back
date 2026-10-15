package main_project.model.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore2Entity;

@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
@Data 
public class Cscore2Dto {

    private Integer cscore2Id;

    private Integer cscore1Id;

    private Double requestWeight;

    private LocalDate desiredDate;

    public Cscore2Entity toEntity(Cscore1Entity cscore1Entity) {

        return Cscore2Entity.builder()
                .cscore1Entity(cscore1Entity)
                .requestWeight(requestWeight)
                .desiredDate(desiredDate)
                .build();
    }

    public static Cscore2Dto from( Cscore2Entity entity ) {
        
        return Cscore2Dto.builder()
                .cscore2Id(entity.getCscore2Id())
                .cscore1Id(entity.getCscore1Entity().getCscore1Id())
                .requestWeight(entity.getRequestWeight())
                .desiredDate(entity.getDesiredDate())
                .build();

    }
    
}
