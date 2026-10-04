package main_project.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradestatusDto {

    // 조회 연도
    private Integer year;

    // 누적 집계 마지막 월
    private Integer throughMonth;

    // 누적 수출액
    private BigDecimal expDlr;

    // 누적 수입액
    private BigDecimal impDlr;

    // 누적 무역수지
    private BigDecimal balPayments;

    // 전년 대비 수출액 증감률(%)
    private BigDecimal expDlrRate;

    // 전년 대비 수입액 증감률(%)
    private BigDecimal impDlrRate;

    // 전년 대비 무역수지 증감률(%)
    private BigDecimal balPaymentsRate;
}