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
public class ExchangeDto {
    private String searchdate;

    private String cur_unit;

    private BigDecimal deal_bas_r;
}
