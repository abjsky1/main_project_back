package main_project.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import main_project.model.dto.ExchangeApiDto;
import main_project.model.dto.ExchangerateDto;

@Service
public class ExchangerateService {

    @Value("${api.public-data.service-key}")
    private String serviceKey;

    // WebClient 객체 생성
    private final WebClient webClient = WebClient.builder().build();

    public List<ExchangerateDto> getExchangerate() {

        // 1. 최신 환율 조회
        String url = "https://api.currencybeacon.com/v1/latest";
        url += "?api_key=" + serviceKey;
        url += "&base=USD";
        url += "&symbols=KRW,EUR,CNH,JPY";

        ExchangeApiDto body = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(ExchangeApiDto.class)
                .block();

        if (body == null || body.getResponse() == null) {
            throw new IllegalStateException("최신 환율 응답이 없습니다.");
        }

        ExchangeApiDto response = body.getResponse();

        validate(response);

        // 최신 환율 목록
        Map<String, BigDecimal> rates = response.getRates();

        // 2. 최신 데이터의 UTC 날짜에서 하루 전 계산
        LocalDate previousDate = parseDate(response.getDate())
                .minusDays(1);

        // 3. 전일 환율 조회
        String previousUrl = "https://api.currencybeacon.com/v1/historical";
        previousUrl += "?api_key=" + serviceKey;
        previousUrl += "&base=USD";
        previousUrl += "&symbols=KRW,EUR,CNH,JPY";
        previousUrl += "&date=" + previousDate;

        ExchangeApiDto previousBody = webClient.get()
                .uri(previousUrl)
                .retrieve()
                .bodyToMono(ExchangeApiDto.class)
                .block(Duration.ofSeconds(20));

        if (previousBody == null || previousBody.getResponse() == null) {
            throw new IllegalStateException("전일 환율 응답이 없습니다.");
        }

        ExchangeApiDto previousResponse = previousBody.getResponse();

        validate(previousResponse);

        // 요청한 날짜와 응답 날짜 확인
        if (!previousDate.equals(parseDate(previousResponse.getDate()))) {
            throw new IllegalStateException("전일 환율 날짜가 다릅니다.");
        }

        // 전일 환율 목록
        Map<String, BigDecimal> previousRates = previousResponse.getRates();

        // 4. 최신 환율을 원화 기준으로 계산
        BigDecimal krw = rates.get("KRW");
        Map<String, BigDecimal> newrate = new LinkedHashMap<>();

        // USD는 1달러당 원화
        newrate.put("USD", krw);

        // EUR, CNH는 통화 1단위당 원화
        for (String code : List.of("EUR", "CNH")) {
            newrate.put(code, krw.divide(rates.get(code), MathContext.DECIMAL128));
        }

        // JPY는 100엔당 원화
        newrate.put("JPY(100)", krw.multiply(BigDecimal.valueOf(100)) .divide(rates.get("JPY"),MathContext.DECIMAL128));

        // 5. 전일 환율도 같은 원화 기준으로 계산
        BigDecimal previousKrw = previousRates.get("KRW");
        Map<String, BigDecimal> previousNewrate =
                new LinkedHashMap<>();

        previousNewrate.put("USD", previousKrw);

        for (String code : List.of("EUR", "CNH")) {
            previousNewrate.put(code,previousKrw.divide(previousRates.get(code),MathContext.DECIMAL128));
        }

        previousNewrate.put("JPY(100)", previousKrw.multiply(BigDecimal.valueOf(100)).divide(previousRates.get("JPY"),MathContext.DECIMAL128));

        // 6. 통화별 증감률 계산 및 결과 반환
        List<ExchangerateDto> result = new ArrayList<>();

        for (String code : List.of("USD", "EUR", "CNH", "JPY(100)")) {

            BigDecimal money = newrate.get(code);
            BigDecimal previousMoney = previousNewrate.get(code);

            // 증감률 = (최신 환율 - 전일 환율) × 100 ÷ 전일 환율
            BigDecimal inderate = money.subtract(previousMoney)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousMoney, 2, RoundingMode.HALF_UP);

            ExchangerateDto dto = ExchangerateDto.builder()
                    .currency(code)
                    .money(money.setScale(3, RoundingMode.HALF_UP))
                    .inderate(inderate)
                    .build();

            result.add(dto);
        }

        return result;
    }

    // 최신·전일 응답 공통 검증
    private void validate(ExchangeApiDto response) {

        if (response == null
                || response.getDate() == null
                || !"USD".equals(response.getBase())
                || response.getRates() == null) {
            throw new IllegalStateException("환율 데이터를 확인해 주세요.");
        }

        for (String code : List.of("KRW", "EUR", "CNH", "JPY")) {
            BigDecimal rate = response.getRates().get(code);

            if (rate == null || rate.signum() <= 0) {
                throw new IllegalStateException("잘못된 환율: " + code);
            }
        }
    }

    // 날짜만 있는 응답과 날짜·시간이 있는 응답 모두 처리
    private LocalDate parseDate(String date) {

        // 예: 2026-10-05
        if (date.length() == 10) {
            return LocalDate.parse(date);
        }

        // 예: 2026-10-06T08:00:00Z
        return OffsetDateTime.parse(date)
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDate();
    }
}