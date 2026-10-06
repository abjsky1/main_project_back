package main_project.service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

import jakarta.annotation.PostConstruct;
import main_project.model.dto.MonthlyTradeDto;
import main_project.model.dto.TradestatusDto;

@Service
public class TradestatusService {

    // CSV 파일이 저장된 폴더
    private final String folderPath = "C:/mpdata";

    // 모든 연도의 월별 합계를 메모리에 보관
    private Map<YearMonth, TradestatusDto> monthlyCache;

    // Spring 시작 시 CSV를 한 번만 읽어서 집계
    @PostConstruct
    public void initialize() {
        System.out.println("무역 CSV 초기화 시작");

        monthlyCache = readCsv();

        System.out.println(
                "무역 CSV 초기화 완료: "
                        + monthlyCache.size()
                        + "개월"
        );
    }

    // 누적 수출액·수입액·무역수지 및 증감률 조회
    public TradestatusDto getYear(Integer year) {

        if (year == null || year < 2000 || year > 2026) {
            throw new IllegalArgumentException(
                    "조회 연도는 2000~2026년만 가능합니다."
            );
        }

        // CSV를 다시 읽지 않고 캐시 사용
        Map<YearMonth, TradestatusDto> monthly = monthlyCache;

        // 해당 연도에 존재하는 마지막 월 찾기
        int lastMonth = 0;

        for (YearMonth date : monthly.keySet()) {
            if (date.getYear() == year) {
                lastMonth = Math.max(
                        lastMonth,
                        date.getMonthValue()
                );
            }
        }

        if (lastMonth == 0) {
            throw new IllegalArgumentException(
                    "해당 연도 데이터가 없습니다."
            );
        }

        // 조회 연도의 1월부터 마지막 월까지 합산
        TradestatusDto current =
                sum(monthly, year, lastMonth);

        // 전년도도 같은 기간으로 합산
        TradestatusDto previous =
                sum(monthly, year - 1, lastMonth);

        if (current == null) {
            throw new IllegalStateException(
                    "집계 기간 중 누락된 월이 있습니다."
            );
        }

        current.setYear(year);
        current.setThroughMonth(lastMonth);

        // 원본 금액으로 전년 동기 대비 증감률 계산
        // 전년도 데이터가 부족하면 증감률은 null 유지
        if (previous != null) {
            current.setExpDlrRate(
                    rate(
                            previous.getExpDlr(),
                            current.getExpDlr()
                    )
            );

            current.setImpDlrRate(
                    rate(
                            previous.getImpDlr(),
                            current.getImpDlr()
                    )
            );

            current.setBalPaymentsRate(
                    rate(
                            previous.getBalPayments(),
                            current.getBalPayments()
                    )
            );
        }

        // 계산 후 응답 금액만 억 달러로 변환
        current.setExpDlr(toHundredMillion(current.getExpDlr()));

        current.setImpDlr(toHundredMillion(current.getImpDlr()));

        current.setBalPayments(toHundredMillion(current.getBalPayments()));

        return current;
    }

    // 추가: 월별 수출액·수입액·무역수지 조회
    public List<MonthlyTradeDto> getMonthly(Integer year) {

        if (year == null || year < 2000 || year > 2026) {
            throw new IllegalArgumentException(
                    "조회 연도는 2000~2026년만 가능합니다."
            );
        }

        List<MonthlyTradeDto> result = new ArrayList<>();

        // 1월부터 12월까지 순서대로 조회
        for (int month = 1; month <= 12; month++) {

            // 기존 월별 캐시 사용
            TradestatusDto data =
                    monthlyCache.get(YearMonth.of(year, month));

            // 없는 월은 0으로 처리하지 않고 제외
            if (data == null) {
                continue;
            }

            // 캐시는 수정하지 않고 새 DTO로 반환
            MonthlyTradeDto dto = MonthlyTradeDto.builder()
                    .year(year)
                    .month(month)
                    .expDlr(
                            toHundredMillion(data.getExpDlr())
                    )
                    .impDlr(
                            toHundredMillion(data.getImpDlr())
                    )
                    .balPayments(
                            toHundredMillion(data.getBalPayments())
                    )
                    .build();

            result.add(dto);
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("해당 연도 데이터가 없습니다.");
        }

        return result;
    }

    // CSV를 읽어 같은 연월의 금액끼리 합산
    private Map<YearMonth, TradestatusDto> readCsv() {

        Map<YearMonth, TradestatusDto> monthly = new HashMap<>();

        DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu.MM");

        // 폴더 안의 CSV 파일 선택
        File[] files = new File(folderPath).listFiles(
                file -> file.isFile()
                        && file.getName()
                                .toLowerCase(java.util.Locale.ROOT)
                                .endsWith(".csv")
        );

        if (files == null || files.length == 0) {
            throw new IllegalStateException("CSV 파일이 없습니다. 경로: " + folderPath);
        }

        for (File file : files) {

            System.out.println("CSV 읽는 중: " + file.getName());

            try (CSVReader reader = new CSVReader(
                    Files.newBufferedReader(
                            file.toPath(),
                            StandardCharsets.UTF_8
                    ))) {

                // 첫 행의 컬럼명 제외
                reader.readNext();

                String[] row;

                while ((row = reader.readNext()) != null) {

                    // 빈 행 건너뛰기
                    if (row.length == 1 && row[0].trim().isEmpty()) {
                        continue;
                    }

                    if (row.length < 10) {
                        throw new IllegalArgumentException("CSV 컬럼 부족");
                    }

                    // 예: 2026.01
                    YearMonth month = YearMonth.parse(
                            row[9].trim(),
                            format
                    );

                    TradestatusDto total =
                            monthly.computeIfAbsent(
                                    month,
                                    key -> emptyTrade()
                            );

                    // 수출액
                    total.setExpDlr(
                            total.getExpDlr()
                                    .add(amount(row[1]))
                    );

                    // 수입액
                    total.setImpDlr(
                            total.getImpDlr()
                                    .add(amount(row[4]))
                    );

                    // 무역수지
                    total.setBalPayments(
                            total.getBalPayments()
                                    .add(amount(row[0]))
                    );
                }

            } catch (Exception e) {
                throw new IllegalStateException(
                        "CSV 읽기 실패: " + file.getName(),
                        e
                );
            }
        }

        return monthly;
    }

    // 특정 연도의 1월부터 마지막 월까지 합산
    private TradestatusDto sum(
            Map<YearMonth, TradestatusDto> monthly,
            int year,
            int lastMonth) {

        // 캐시의 DTO를 수정하지 않고 새 DTO에 합산
        TradestatusDto total = emptyTrade();

        for (int month = 1; month <= lastMonth; month++) {

            TradestatusDto data =
                    monthly.get(YearMonth.of(year, month));

            // 한 달이라도 없으면 불완전한 기간으로 처리
            if (data == null) {
                return null;
            }

            total.setExpDlr(
                    total.getExpDlr().add(data.getExpDlr())
            );

            total.setImpDlr(
                    total.getImpDlr().add(data.getImpDlr())
            );

            total.setBalPayments(
                    total.getBalPayments()
                            .add(data.getBalPayments())
            );
        }

        return total;
    }

    // 금액이 0인 DTO 생성
    private TradestatusDto emptyTrade() {
        return TradestatusDto.builder()
                .expDlr(BigDecimal.ZERO)
                .impDlr(BigDecimal.ZERO)
                .balPayments(BigDecimal.ZERO)
                .build();
    }

    // 공백과 천 단위 쉼표 제거
    private BigDecimal amount(String value) {
        return new BigDecimal(
                value.trim().replace(",", "")
        );
    }

    // 증감률: (현재 - 이전) / |이전| × 100
    private BigDecimal rate(
            BigDecimal previous,
            BigDecimal current) {

        if (previous.signum() == 0) {
            return null;
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        previous.abs(),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    // 달러를 억 달러로 변환
    private BigDecimal toHundredMillion(BigDecimal amount) {
        return amount.divide(
                new BigDecimal("100000000"),
                1,
                RoundingMode.HALF_UP
        );
    }
}