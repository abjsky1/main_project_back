package main_project.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TradeSearchDto {
    // HS코드
    private String hsCode;
    // 품목명
    private String itemName;
    // 국가 
    private String country;
    // 수출액
    private BigDecimal exportAmount;
    // 수입액
    private BigDecimal importAmount;
    // 무역수지
    private BigDecimal tradeBalance;
    // 기간
    private String period;
}