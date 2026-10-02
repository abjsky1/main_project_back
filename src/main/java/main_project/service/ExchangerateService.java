package main_project.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import main_project.model.dto.ExchangerateDto;

@Service 
public class ExchangerateService {
    @Value("${api.public-data.service-key}")
    private String serviceKey;
    // WebClient 객체 빌더패턴 생성 
    public final WebClient webClient = WebClient.builder().build();
    public ExchangerateDto getExchangerate(){
        String url = "https://api.currencybeacon.com/v1/latest";
        url += "?api_key="+serviceKey;
        url += "&base=USD";
        url += "&symbols="+"KRW,EUR,CNH,JPY";

        // api를 Dto로 받기
        ExchangerateDto response = webClient.get()
            .uri(url) // uri는 http 주소상에 자원(쿼리스트링) 까지 포함
            .retrieve() // 요청 결과 반환 결과 수신
            .bodyToMono(ExchangerateDto.class) // 응답 결과  JSON -> ExchangeDto
            .block(); // 동기화

        // DTO에서 환율 목록 가져오기
        Map<String, BigDecimal> rates = response.getRates();

        // EUR,CNH,JPY,KRW 환율 계산
        for(String code : List.of("KRW","EUR","CNH","JPY")){
            BigDecimal rate = rates.get(code);
       
            if (rate == null || rate.signum() <= 0) {
                throw new IllegalStateException("잘못된 환율: " + code);
            }
        }

        // 통화 1단위당 원화 금액으로 계산
        BigDecimal krw = rates.get("KRW");
        Map<String, BigDecimal> newrate = new LinkedHashMap<>();

        newrate.put("USD", krw.setScale(3,RoundingMode.HALF_UP));
        
        // 통화 코드 하나씩 선택
        for(String code : List.of("EUR","CNH")){
            //원화 기준 환율 계산 후 newtate에 저장하는 반복문
            newrate.put(code, krw.divide(rates.get(code), 3, RoundingMode.HALF_UP));
        }

        // JPY는 100엔 기준
        newrate.put("JPY(100)", krw.multiply(BigDecimal.valueOf(100))
                .divide(rates.get("JPY"), 3, RoundingMode.HALF_UP));

        // 5. 계산한 환율을 새 DTO에 반환
        return new ExchangerateDto(newrate);

    }
}
