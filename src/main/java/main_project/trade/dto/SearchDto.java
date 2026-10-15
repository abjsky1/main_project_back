package main_project.model.dto;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SearchDto {
    // 시작 기간
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth startdate;

    // 종료 기간
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth enddate;

    // 국가
    private String country;

    // HS코드
    private String hscode;
}