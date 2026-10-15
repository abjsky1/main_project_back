package main_project.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder 
@Data 
public class ExchangerateDto {
    // 통화 코드
    private String currency;
    // 환율
    private BigDecimal money;
    // 전일 대비 증감율
    private BigDecimal inderate;
}