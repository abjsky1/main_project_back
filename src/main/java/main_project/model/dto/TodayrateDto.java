package main_project.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
@Data 
public class TodayrateDto {

    // 최신 환율
    private BigDecimal rate;

    // 전날 대비 증감율
    private BigDecimal changerate;
}
