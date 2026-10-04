package main_project.model.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TodayrateApiDto {

    // 최신 응답은 날짜·시간, 과거 응답은 날짜
    private String date;

    // 기준 통화: USD
    private String base;

    // 통화별 환율
    private Map<String, BigDecimal> rates;
}