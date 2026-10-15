package main_project.model.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeApiDto {

    // 외부 API 응답의 response 객체
    private ExchangeApiDto response;

    private String date;

    private String base;

    private Map<String, BigDecimal> rates;
}