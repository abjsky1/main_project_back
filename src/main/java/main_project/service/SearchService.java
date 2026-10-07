package main_project.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import jakarta.annotation.PostConstruct;
import main_project.model.dto.SearchDto;
import main_project.model.dto.TradeSearchDto;

@Service
public class SearchService {

    private final String folderPath = "C:/mpdata";

    // HS코드와 품목명을 연결해서 저장
    private final Map<String, String> itemNames = new HashMap<>();

    // 같은 HS코드에 서로 다른 품목명이 있는 코드
    private final Set<String> conflictingCodes = new HashSet<>();

    @PostConstruct
    public void initialize() {

        ClassPathResource file = new ClassPathResource("static/hscode/hscode.csv");

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // 첫 번째 행은 hscode_id, hscode_name 같은 제목이므로 제외
            reader.readNext();

            // CSV 한 행의 각 열을 담을 배열
            String[] row;

            // 한 행씩 읽고, 더 읽을 행이 없으면 반복 종료
            while ((row = reader.readNext()) != null) {

                // HS코드와 품목명 두 열이 없으면 해당 행 건너뛰기
                if (row.length < 2) {
                    continue;
                }

                // row[0]: HS코드, row[1]: 품목명
                String hsCode = row[0].trim();
                String itemName = row[1].trim();

                // 코드가 비어 있는 행은 제외
                if (hsCode.isEmpty()) {
                    continue;
                }

                String previous = itemNames.putIfAbsent(hsCode, itemName);

                // 품목명이 서로 다르면 확인이 필요한 코드로 기록
                if (previous != null && !previous.equals(itemName)) {
                    conflictingCodes.add(hsCode);
                }
            }

        } catch (IOException | CsvValidationException e) {
            throw new IllegalStateException("품목명 CSV를 읽을 수 없습니다.", e);
        }
    }

    public List<TradeSearchDto> getsearch(SearchDto searchDto) {

        // 요청 DTO에서 검색 조건 꺼내기
        YearMonth start = searchDto.getStartdate();
        YearMonth end = searchDto.getEnddate();
        String country = searchDto.getCountry();
        String hsCode = searchDto.getHscode();

        if (start == null || end == null || hsCode == null || hsCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작 기간, 종료 기간, HS코드를 입력하세요.");
        }

        // 시작 기간이 종료 기간보다 늦으면 검색할 수 없음
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작 기간은 종료 기간보다 늦을 수 없습니다.");
        }

        hsCode = hsCode.trim();

        // 국가를 입력하지 않았다면 전체를 검색
        // 입력했다면 앞뒤 공백 제거
        if (country == null || country.isBlank()) {
            country = "전체";
        } else {
            country = country.trim();
        }

        List<TradeSearchDto> result = new ArrayList<>();

        // 품목명을 하나로 결정할 수 없는 코드는 검색 중단
        if (conflictingCodes.contains(hsCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"해당 HS코드에 여러 품목명이 있습니다. 품목명 CSV를 확인하세요.");
        }

        String itemName = itemNames.get(hsCode);

        // 품목명 CSV에 같은 코드가 없으면 두 파일의 코드가 일치하지 않으므로 빈 목록 반환
        if (itemName == null) {
            return result;
        }

        // 시작 연도부터 종료 연도까지 반복
        for (int year = start.getYear(); year <= end.getYear(); year++) {

            // 예: C:/mpdata/trade_data_2026.csv
            Path file = Path.of(folderPath, "trade_data_" + year + ".csv");

            // 무역 CSV를 UTF-8로 열기
            try (CSVReader reader = new CSVReader(
                    Files.newBufferedReader(
                            file,
                            StandardCharsets.UTF_8
                    ))) {

                // 열 제목이 있는 첫 행 제외
                reader.readNext();

                String[] row;

                // 현재 연도 파일의 데이터를 한 행씩 확인
                while ((row = reader.readNext()) != null) {

                    // 빈 줄이면 건너뛰기
                    if (row.length == 1 && row[0].isBlank()) {
                        continue;
                    }

                    // 아래에서 row[9]까지 사용하므로 최소 10개 열이 필요
                    if (row.length < 10) {
                        throw new IllegalStateException("CSV 열 개수를 확인하세요: " + file);
                    }

                    // 무역 CSV의 HS코드와 검색 HS코드 비교
                    // 다르면 현재 행을 제외하고 다음 행으로 이동
                    if (!row[3].trim().equals(hsCode)) {
                        continue;
                    }

                    // 국가 비교
                    // 전체로 선택하면 국가 구분 없이, 기간과 HS코드가 맞는 모든 행을 조회
                    if (!country.equals("전체") && !row[7].trim().equals(country)) {
                        continue;
                    }

                    // 예: "2026.01" → "2026-01" → YearMonth 객체
                    YearMonth period = YearMonth.parse(row[9].trim().replace(".", "-"));

                    // 시작 월보다 이전이거나 종료 월보다 이후면 제외
                    if (period.isBefore(start) || period.isAfter(end)) {
                        continue;
                    }

                    // 해당 무역 데이터 행과 품목명을 합쳐 DTO 생성
                    result.add(new TradeSearchDto(
                            row[3].trim(),       // HS코드
                            itemName,            // 품목명 
                            row[7].trim(),       // 국가
                            toAmount(row[1]),    // 수출액
                            toAmount(row[4]),    // 수입액
                            toAmount(row[0]),    // 무역수지
                            period.toString()    // 기간
                    ));
                }

            } catch (IOException | CsvValidationException e) {
                // 파일이 없거나 CSV 읽기에 실패하면 오류 발생
                throw new IllegalStateException("무역 CSV를 읽을 수 없습니다: " + file, e);
            }
        }

        return result;
    }

    // CSV에서 읽은 금액 문자열을 BigDecimal로 변환
    // 예: " 1,234 " → "1234" → 숫자 1234
    private BigDecimal toAmount(String value) {
        return new BigDecimal(value.trim().replace(",", ""));
    }
}