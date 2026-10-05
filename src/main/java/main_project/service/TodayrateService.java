package main_project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import main_project.model.dto.TodayrateApiDto;
import main_project.model.dto.TodayrateDto;

@Service
public class TodayrateService {

    @Value("${api.public-data.service-key}")
    private String serviceKey;

    private final WebClient webClient = WebClient.builder().build();

    public List<TodayrateDto> getTodaylist() {

        // 최신 KRW 환율 조회
        String url = "https://api.currencybeacon.com/v1/latest";
        url += "?api_key=" + serviceKey;
        url += "&base=USD";
        url += "&symbols=KRW";

        TodayrateApiDto response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(TodayrateApiDto.class)
                .block();

        validate(response);

        // 최신 환율 가져오기
        BigDecimal rate = response.getRates().get("KRW");

        // 최신 데이터의 UTC 날짜에서 하루 전 계산
        LocalDate previousDate = OffsetDateTime.parse(response.getDate())
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDate()
                .minusDays(1);

        // 전날 KRW 환율 조회
        String previousUrl = "https://api.currencybeacon.com/v1/historical";
        previousUrl += "?api_key=" + serviceKey;
        previousUrl += "&base=USD";
        previousUrl += "&symbols=KRW";
        previousUrl += "&date=" + previousDate;

        TodayrateApiDto previousResponse = webClient.get()
                .uri(previousUrl)
                .retrieve()
                .bodyToMono(TodayrateApiDto.class)
                .block();

        validate(previousResponse);

        // 요청한 날짜와 응답 날짜 확인
        if (!previousDate.toString().equals(previousResponse.getDate())) {
            throw new IllegalStateException("전날 환율 날짜가 다릅니다.");
        }

        BigDecimal previousRate = previousResponse.getRates().get("KRW");

        // 증감률 = (최신 환율 - 전날 환율) × 100 ÷ 전날 환율
        BigDecimal changerate = rate.subtract(previousRate)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousRate, 2, RoundingMode.HALF_UP);

        // 최신 환율과 증감률 반환
        TodayrateDto result = TodayrateDto.builder()
                .rate(rate.setScale(1, RoundingMode.HALF_UP))
                .changerate(changerate)
                .build();

        return List.of(result);
    }

    // 최신·전날 응답에 공통으로 사용하는 검증
    private void validate(TodayrateApiDto response) {

        if (response == null
                || response.getDate() == null
                || !"USD".equals(response.getBase())
                || response.getRates() == null
                || response.getRates().get("KRW") == null
                || response.getRates().get("KRW").signum() <= 0) {

            throw new IllegalStateException("환율 데이터를 확인해 주세요.");
        }
    }
}