package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeYearDto {

    // 연도
    private int year;

    // 연도 월평균 무역수지
    private double balPayments;

    // 연도 월평균 수출액
    private double expDlr;

    // 연도 월평균 수입액
    private double impDlr;

    // 전년도 대비 무역수지 증감률
    private Double balPaymentsRate;

    // 전년도 대비 수출액 증감률
    private Double expDlrRate;

    // 전년도 대비 수입액 증감률
    private Double impDlrRate;
}