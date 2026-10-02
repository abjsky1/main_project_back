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
@Data 
@Builder 
@JsonIgnoreProperties(ignoreUnknown = true) //rate,response,base,date 필드는 무시 
public class ExchangerateDto {

    private Map<String, BigDecimal> rates;

}
