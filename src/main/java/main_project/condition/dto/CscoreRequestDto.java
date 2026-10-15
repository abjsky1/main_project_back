package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor 
@Builder 
@Data 
// Controller에서 등록 요청을 한 번에 받기 위한 묶음 DTO
public class CscoreRequestDto {

    private Cscore1Dto cscore1Dto;

    private Cscore2Dto cscore2Dto;

    private Cscore3Dto cscore3Dto;

}
