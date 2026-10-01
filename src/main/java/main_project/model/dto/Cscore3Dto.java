package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore3Entity;

@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
@Data 
public class Cscore3Dto {

    private Integer cscore3Id;

    private Integer cscore1Id;

    private Boolean generalContainer;

    private Boolean refrigerated;

    private Boolean dangerous;

    private Boolean heavyCargo;

    private Boolean specialCargo;

    public Cscore3Entity toEntity(Cscore1Entity cscore1Entity) {

        return Cscore3Entity.builder()
                .cscore1Entity(cscore1Entity)
                .generalContainer(generalContainer)
                .refrigerated(refrigerated)
                .dangerous(dangerous)
                .heavyCargo(heavyCargo)
                .specialCargo(specialCargo)
                .build();

    }

    public static Cscore3Dto from( Cscore3Entity entity ) {

        return Cscore3Dto.builder()
                .cscore3Id(entity.getCscore3Id())
                .cscore1Id(entity.getCscore1Entity().getCscore1Id())
                .generalContainer(entity.getGeneralContainer())
                .refrigerated(entity.getRefrigerated())
                .dangerous(entity.getDangerous())
                .heavyCargo(entity.getHeavyCargo())
                .specialCargo(entity.getSpecialCargo())
                .build();

    }

}
