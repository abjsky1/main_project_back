package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.Lscore3Entity;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Lscore3Dto {

    private Integer lscore3Id;

    private Integer lscore1Id;

    private Boolean generalContainer;

    private Boolean refrigerated;

    private Boolean dangerous;

    private Boolean heavyCargo;

    private Boolean specialCargo;


    // DTO -> Entity
    public Lscore3Entity toEntity(Lscore1Entity lscore1Entity) {

        return Lscore3Entity.builder()
                .lscore1Entity(lscore1Entity)
                .generalContainer(generalContainer)
                .refrigerated(refrigerated)
                .dangerous(dangerous)
                .heavyCargo(heavyCargo)
                .specialCargo(specialCargo)
                .build();

    }

    // Entity -> DTO
    public static Lscore3Dto from(Lscore3Entity entity) {

        return Lscore3Dto.builder()
                .lscore3Id(entity.getLscore3Id())
                .lscore1Id(entity.getLscore1Entity().getLscore1Id())
                .generalContainer(entity.getGeneralContainer())
                .refrigerated(entity.getRefrigerated())
                .dangerous(entity.getDangerous())
                .heavyCargo(entity.getHeavyCargo())
                .specialCargo(entity.getSpecialCargo())
                .build();

    }

}