package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LscoreRequestDto {

    private Lscore1Dto lscore1Dto;

    private Lscore2Dto lscore2Dto;

    private Lscore3Dto lscore3Dto;

}
