package main_project.service;

import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import main_project.model.dto.ExchangeDto;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180ParserBuilder;

@Service 
public class ExchangeService {

    public List<ExchangeDto> month() {

        // "2012-01/USD" → 해당 월·통화의 일별 환율 목록
        Map<String, List<BigDecimal>> groups = new TreeMap<>();

        // 연도별 CSV 읽기
        for (int year = 2000; year <= 2026; year++) {
            readCsv(year, groups);
        }

        List<ExchangeDto> list = new ArrayList<>();

        // 월·통화별 평균 계산
        for (String key : groups.keySet()) {
            List<BigDecimal> rates = groups.get(key);

            BigDecimal sum = BigDecimal.ZERO;

            for (BigDecimal rate : rates) {
                sum = sum.add(rate);
            }

            BigDecimal average = sum.divide(
                    BigDecimal.valueOf(rates.size()),
                    3,
                    RoundingMode.HALF_UP
            );

            String[] parts = key.split("/", 2);
            String currency = parts[1];

            // IDR(100) 월 평균을 1통화 단위로 변환
            if ("IDR(100)".equals(currency)) {
                average = average.movePointLeft(2); // 100으로 나누기
                currency = currency.replace("(100)", "");
            }

            ExchangeDto row = new ExchangeDto(
                parts[0], // searchdate: 연월
                currency, // cur_unit: 통화 코드
                average   // deal_bas_r: 월 평균 환율
            );

            list.add(row);
        }

        return list;
    }

    private void readCsv(
            int year,
            Map<String, List<BigDecimal>> groups
    ) {
        String fileName = "static/exchange_rate/exchange_rate_" + year + ".csv";
        ClassPathResource resource = new ClassPathResource(fileName);

        try (PushbackReader reader = new PushbackReader(
                new InputStreamReader(
                        resource.getInputStream(),
                        StandardCharsets.UTF_8
                ), 1
        )) {
            // UTF-8 BOM 제거
            int first = reader.read();

            if (first != -1 && first != '\uFEFF') {
                reader.unread(first);
            }

            try (CSVReader csv = new CSVReaderBuilder(reader)
                    .withCSVParser(new RFC4180ParserBuilder().build())
                    .build()) {

                // 첫 행에서 컬럼명 읽기
                List<String> headers = Arrays.stream(csv.readNext())
                        .map(String::trim)
                        .toList();

                int dateIndex = headers.indexOf("searchdate");
                int unitIndex = headers.indexOf("cur_unit");
                int rateIndex = headers.indexOf("deal_bas_r");

                // [추가한 부분] 필요한 헤더를 찾았는지 확인
                if (dateIndex < 0 || unitIndex < 0 || rateIndex < 0) {
                    throw new IllegalArgumentException(
                            fileName + " 헤더 확인: " + headers
                    );
                }

                String[] values;

                while ((values = csv.readNext()) != null) {
                    String date = values[dateIndex]
                            .trim()
                            .replace("-", "");

                    String month = date.substring(0, 4)
                            + "-" + date.substring(4, 6);

                    String key = month + "/" + values[unitIndex].trim();

                    String rateText = values[rateIndex].trim().replace(",", "");

                    // 환율 값이 비어 있으면 다음 행으로 넘어가기
                    if (rateText.isEmpty()) {
                        continue;
                    }

                    BigDecimal rate = new BigDecimal(rateText);

                    if (!groups.containsKey(key)) {
                        groups.put(key, new ArrayList<>());
                    }

                    groups.get(key).add(rate);
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    fileName + " 처리 실패", e
            );
        }
    }
}
