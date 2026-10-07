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

    private final String folderPath = "C:/mpdata";

    // 연월별 합계 저장
    private final Map<YearMonth, TradestatusDto> monthlyCache = new HashMap<>();

    // 1. 서버 시작 시 CSV를 한 번 읽어서 월별 합계 저장
    @PostConstruct
    public void initialize() {

        File[] files = new File(folderPath).listFiles(
                file -> file.isFile() && file.getName()
                        .toLowerCase(java.util.Locale.ROOT)
                        .endsWith(".csv")
        );

        if (files == null || files.length == 0) {
            throw new IllegalStateException("CSV 파일이 없습니다: " + folderPath);
        }

        DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu.MM");

        monthlyCache.clear();

        for (File file : files) {
            try (CSVReader reader = new CSVReader(
                    Files.newBufferedReader(file.toPath(),StandardCharsets.UTF_8
                ))) {

                reader.readNext(); // 컬럼명 제외
                String[] row;

                while ((row = reader.readNext()) != null) {

                    if (row.length == 1 && row[0].trim().isEmpty()) {
                        continue;
                    }

                    if (row.length < 10) {
                        throw new IllegalArgumentException("CSV 컬럼 부족");
                    }

                    YearMonth date = YearMonth.parse(row[9].trim(), format);

                    // 해당 월이 처음 나오면 합계 객체 생성
                    TradestatusDto total = monthlyCache.get(date);

                    if (total == null) {
                        total = emptyTrade();
                        monthlyCache.put(date, total);
                    }

                    addAmounts(
                            total,
                            amount(row[1]), // 수출액
                            amount(row[4]), // 수입액
                            amount(row[0])  // 무역수지
                    );
                }

            } catch (Exception e) {
                throw new IllegalStateException("CSV 읽기 실패: " + file.getName(), e);
            }
        }

        System.out.println("무역 CSV 초기화 완료: " + monthlyCache.size() + "개월");
    }

    // 2. 누적 수출입·무역수지·전년 동기 증감률 조회
    public TradestatusDto getYear(Integer year) {

        validateYear(year);

        // 해당 연도의 마지막 집계 월
        int lastMonth = 0;

        for (int month = 1; month <= 12; month++) {
            if (monthlyCache.containsKey(YearMonth.of(year, month))) {
                lastMonth = month;
            }
        }

        if (lastMonth == 0) {
            throw new IllegalArgumentException("해당 연도 데이터가 없습니다.");
        }

        TradestatusDto current = sum(year, lastMonth);
        TradestatusDto previous = sum(year - 1, lastMonth);

        if (current == null) {
            throw new IllegalStateException("집계 기간 중 누락된 월이 있습니다.");
        }

        current.setYear(year);
        current.setThroughMonth(lastMonth);

        // 전년도 같은 기간이 모두 있을 때만 증감률 계산
        if (previous != null) {
            current.setExpDlrRate(
                    rate(previous.getExpDlr(), current.getExpDlr())
            );
            current.setImpDlrRate(
                    rate(previous.getImpDlr(), current.getImpDlr())
            );
            current.setBalPaymentsRate(
                    rate(previous.getBalPayments(), current.getBalPayments())
            );
        }

        // 증감률 계산 후 표시 금액을 억 달러로 변환
        current.setExpDlr(toHundredMillion(current.getExpDlr()));
        current.setImpDlr(toHundredMillion(current.getImpDlr()));
        current.setBalPayments(toHundredMillion(current.getBalPayments()));

        return current;
    }

    // 3. 월별 수출입·무역수지 조회
    public List<MonthlyTradeDto> getMonthly(Integer year) {

        validateYear(year);

        List<MonthlyTradeDto> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {

            TradestatusDto data =
                    monthlyCache.get(YearMonth.of(year, month));

            if (data == null) {
                continue;
            }

            result.add(MonthlyTradeDto.builder()
                            .year(year)
                            .month(month)
                            .expDlr(toHundredMillion(data.getExpDlr()))
                            .impDlr(toHundredMillion(data.getImpDlr()))
                            .balPayments(toHundredMillion(data.getBalPayments()))
                            .build()
            );
        }

        if (result.isEmpty()) {throw new IllegalArgumentException("해당 연도 데이터가 없습니다.");}

        return result;
    }

    // 지정 연도의 1월부터 마지막 월까지 합산
    private TradestatusDto sum(int year, int lastMonth) {

        TradestatusDto total = emptyTrade();

        for (int month = 1; month <= lastMonth; month++) {

            TradestatusDto data = monthlyCache.get(YearMonth.of(year, month));

            if (data == null) {
                return null;
            }

            addAmounts(
                    total,
                    data.getExpDlr(),
                    data.getImpDlr(),
                    data.getBalPayments()
            );
        }

        return total;
    }

    // 합계에 수출액·수입액·무역수지 더하기
    private void addAmounts(
            TradestatusDto total,
            BigDecimal export,
            BigDecimal imports,
            BigDecimal balance) {

        total.setExpDlr(total.getExpDlr().add(export));
        total.setImpDlr(total.getImpDlr().add(imports));
        total.setBalPayments(total.getBalPayments().add(balance));
    }

    // 합산 시작값 생성
    private TradestatusDto emptyTrade() {
        return TradestatusDto.builder()
                .expDlr(BigDecimal.ZERO)
                .impDlr(BigDecimal.ZERO)
                .balPayments(BigDecimal.ZERO)
                .build();
    }

    private void validateYear(Integer year) {
        if (year == null || year < 2000 || year > 2026) {
            throw new IllegalArgumentException("조회 연도는 2000~2026년만 가능합니다.");
        }
    }

    // CSV 금액을 숫자로 변환
    private BigDecimal amount(String value) {
        return new BigDecimal(value.trim().replace(",", ""));
    }

    // 증감률: (현재 - 이전) / |이전| × 100
    private BigDecimal rate(BigDecimal previous, BigDecimal current) {
        if (previous.signum() == 0) {
            return null;
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
    }

    // 달러 → 억 달러
    private BigDecimal toHundredMillion(BigDecimal value) {
        return value.divide(
                new BigDecimal("100000000"),
                1,
                RoundingMode.HALF_UP
        );
    }
}