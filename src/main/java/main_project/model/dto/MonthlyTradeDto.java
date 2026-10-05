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
public class MonthlyTradeDto {
    private Integer year;

    private Integer month;

    private BigDecimal expDlr;

    private BigDecimal impDlr;
    
    private BigDecimal balPayments;
}
