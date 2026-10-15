package main_project.trade.service;

import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;

import main_project.insight.dto.InsightDto.Point;
import main_project.insight.dto.InsightDto.Country;
import main_project.insight.dto.InsightDto.DataFile;
import main_project.insight.dto.InsightDto.Dataset;
import main_project.insight.dto.InsightDto.Item;
import main_project.insight.dto.InsightDto.Request;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TradeCodeCatalog {

    private static final String[] TRADE_HEADER = {
            "balPayments",
            "expDlr",
            "expWgt",
            "hsCd",
            "impDlr",
            "impWgt",
            "statCd",
            "statCdCntnKor1",
            "statKor",
            "year"
    };

    // 무역 CSV 저장 폴더
    private final String folderPath = "C:/mpdata";

    // 보유 데이터 연도 범위
    private final int firstYear = 2000;
    private final int lastYear = 2026;

    // 9자리 코드의 앞자리 0 복원 여부
    private final boolean restoreLeadingZero = true;

    private final Map<Integer, Country> countries = new TreeMap<>();

    private final Map<String, String> itemNames = new TreeMap<>();
    private final Map<String, Set<String>> aliases = new HashMap<>();
    private final Map<String, YearMonth> namePeriods = new HashMap<>();

    private final NavigableSet<YearMonth> periods = new TreeSet<>();

    @PostConstruct
    public void initialize() throws Exception {

        if (firstYear < 2000 || lastYear < firstYear) {
            throw new IllegalStateException(
                    "무역 데이터 연도 설정이 잘못되었습니다."
            );
        }

        loadCountries();
        loadReferenceItems();
        loadTradeCatalog();
    }

    // 국가 기준 정보
    private void loadCountries() throws Exception {

        try (CSVReader reader =
                     resource("static/country/country.csv")) {

            reader.readNext();

            Set<String> usedCodes = new HashSet<>();
            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row.length < 4 || row[0].isBlank()) {
                    continue;
                }

                int id = Integer.parseInt(clean(row[0]));
                String code = clean(row[1]);
                String name = row[3].trim();

                if (code.isBlank() || name.isBlank()) {
                    throw new IllegalArgumentException(
                            "국가 기준 정보가 비어 있습니다."
                    );
                }

                if (countries.containsKey(id) || !usedCodes.add(code)) {
                    throw new IllegalArgumentException(
                            "국가 ID 또는 국가 코드가 중복됩니다."
                    );
                }

                countries.put(
                        id,
                        Country.builder()
                                .id(id)
                                .code(code)
                                .name(name)
                                .build()
                );
            }
        }
    }

    // 품목 기준 정보
    private void loadReferenceItems() throws Exception {

        try (CSVReader reader =
                     resource("static/hscode/hscode.csv")) {

            reader.readNext();

            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row.length < 2) {
                    continue;
                }

                String code = normalize(row[0]);
                String name = row[1].trim();

                if (code == null || name.isBlank()) {
                    continue;
                }

                aliases.computeIfAbsent(
                        code,
                        ignored -> new TreeSet<>()
                ).add(name);

                itemNames.putIfAbsent(code, name);
            }
        }
    }

    // 실제 CSV에 있는 품목과 기간 확인
    private void loadTradeCatalog() throws Exception {

        for (int year = firstYear; year <= lastYear; year++) {
            Path file = file(year);

            if (!Files.exists(file)) {
                continue;
            }

            try (CSVReader reader = csv(file)) {
                validateHeader(reader.readNext());

                String[] row;

                while ((row = reader.readNext()) != null) {
                    validateRow(row);

                    YearMonth month = parseMonth(row[9]);

                    if (month.getYear() != year) {
                        throw new IllegalArgumentException(
                                file.getFileName()
                                        + ": 파일 연도와 행의 연도가 다릅니다."
                        );
                    }

                    periods.add(month);

                    String code = normalize(row[3]);
                    String name = row[8].trim();

                    if (code != null && !name.isBlank()) {
                        registerItem(code, name, month);
                    }
                }
            }
        }
    }

    private void registerItem(
            String code,
            String name,
            YearMonth month
    ) {

        aliases.computeIfAbsent(
                code,
                ignored -> new TreeSet<>()
        ).add(name);

        YearMonth previousMonth = namePeriods.get(code);
        String previousName = itemNames.get(code);

        boolean replace = previousMonth == null
                || month.isAfter(previousMonth)
                || (month.equals(previousMonth)
                    && name.compareTo(previousName) < 0);

        if (replace) {
            namePeriods.put(code, month);
            itemNames.put(code, name);
        }
    }

    public String normalize(String raw) {

        if (raw == null) {
            return null;
        }

        String code = clean(raw);

        if (code.matches("[0-9]{10}")) {
            return code;
        }

        if (restoreLeadingZero && code.matches("[0-9]{9}")) {
            return "0" + code;
        }

        return null;
    }

    public List<Country> countries() {
        return List.copyOf(countries.values());
    }

    public Country country(int id) {
        return countries.get(id);
    }

    public String itemName(String code) {
        return itemNames.get(code);
    }

    public YearMonth earliest() {
        return periods.isEmpty() ? null : periods.first();
    }

    public YearMonth latest() {
        return periods.isEmpty() ? null : periods.last();
    }

    public List<Item> searchItems(String query) {

        if (query == null) {
            return List.of();
        }

        String keyword = query.trim().toLowerCase(Locale.ROOT);

        if (keyword.length() < 2 || keyword.length() > 80) {
            return List.of();
        }

        return itemNames.keySet()
                .stream()
                .filter(code ->
                        code.contains(keyword)
                                || aliases.getOrDefault(code, Set.of())
                                .stream()
                                .anyMatch(name ->
                                        name.toLowerCase(Locale.ROOT)
                                                .contains(keyword)
                                )
                )
                .limit(30)
                .map(code -> Item.builder()
                        .code(code)
                        .name(itemNames.get(code))
                        .build()
                )
                .toList();
    }

    // 선택 조건과 전년 동기 자료 조회
    public Dataset read(Request request) throws Exception {

        YearMonth start = YearMonth.parse(request.getStart());
        YearMonth end = YearMonth.parse(request.getEnd());
        YearMonth readStart = start.minusYears(1);

        Map<Integer, Map<YearMonth, Point>> monthly =
                new LinkedHashMap<>();

        Map<String, Integer> selectedCodes = new HashMap<>();

        for (Integer id : request.getCountryIds()) {
            monthly.put(id, new TreeMap<>());
            selectedCodes.put(country(id).getCode(), id);
        }

        Set<String> observedNames = new TreeSet<>();
        List<DataFile> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (int year = readStart.getYear();
             year <= end.getYear();
             year++) {

            if (year < firstYear || year > lastYear) {
                warnings.add(
                        year + "년은 설정된 자료 범위 밖입니다."
                );
                continue;
            }

            Path file = file(year);
            DataFile before = describe(file);
            files.add(before);

            if (!before.isExists()) {
                warnings.add(
                        file.getFileName() + " 파일이 없습니다."
                );
                continue;
            }

            try (CSVReader reader = csv(file)) {
                validateHeader(reader.readNext());

                String[] row;

                while ((row = reader.readNext()) != null) {
                    validateRow(row);

                    if (!request.getHsCode().equals(normalize(row[3]))) {
                        continue;
                    }

                    Integer countryId =
                            selectedCodes.get(clean(row[6]));

                    if (countryId == null) {
                        continue;
                    }

                    YearMonth month = parseMonth(row[9]);

                    if (month.getYear() != year) {
                        throw new IllegalArgumentException(
                                "파일 연도와 행의 연도가 다릅니다."
                        );
                    }

                    if (month.isBefore(readStart)
                            || month.isAfter(end)) {
                        continue;
                    }

                    Map<YearMonth, Point> countryMonths =
                            monthly.get(countryId);

                    // 국가·HS 코드·월당 한 행인 자료를 전제로 한다.
                    if (countryMonths.containsKey(month)) {
                        throw new IllegalArgumentException(
                                "국가·HS 코드·월이 같은 행이 중복됩니다."
                        );
                    }

                    countryMonths.put(
                            month,
                            Point.builder()
                                    .exportAmount(amount(row[1]))
                                    .importAmount(amount(row[4]))
                                    .build()
                    );

                    if (!row[8].isBlank()) {
                        observedNames.add(row[8].trim());
                    }
                }
            }

            if (!before.equals(describe(file))) {
                throw new IllegalStateException(
                        "조회 중 무역 파일이 변경되었습니다."
                );
            }
        }

        return Dataset.builder()
                .monthly(monthly)
                .observedItemNames(List.copyOf(observedNames))
                .files(files)
                .limitations(warnings)
                .build();
    }

    private DataFile describe(Path file) throws Exception {

        if (!Files.exists(file)) {
            return DataFile.builder()
                    .name(file.getFileName().toString())
                    .exists(false)
                    .build();
        }

        return DataFile.builder()
                .name(file.getFileName().toString())
                .exists(true)
                .size(Files.size(file))
                .modifiedAt(
                        Files.getLastModifiedTime(file)
                                .toInstant()
                                .toString()
                )
                .build();
    }

    private Path file(int year) {
        return Path.of(
                folderPath,
                "trade_data_" + year + ".csv"
        );
    }

    private CSVReader csv(Path file) throws Exception {
        return new CSVReader(
                Files.newBufferedReader(
                        file,
                        StandardCharsets.UTF_8
                )
        );
    }

    private CSVReader resource(String path) throws Exception {
        return new CSVReader(
                new InputStreamReader(
                        new ClassPathResource(path).getInputStream(),
                        StandardCharsets.UTF_8
                )
        );
    }

    private void validateHeader(String[] header) {

        if (header == null || header.length != TRADE_HEADER.length) {
            throw new IllegalArgumentException(
                    "무역 CSV 헤더 열 개수가 다릅니다."
            );
        }

        for (int i = 0; i < TRADE_HEADER.length; i++) {
            if (!TRADE_HEADER[i].equals(clean(header[i]))) {
                throw new IllegalArgumentException(
                        "무역 CSV 헤더를 확인하세요: "
                                + TRADE_HEADER[i]
                );
            }
        }
    }

    private void validateRow(String[] row) {
        if (row.length != TRADE_HEADER.length) {
            throw new IllegalArgumentException(
                    "무역 CSV 행의 열 개수가 다릅니다."
            );
        }
    }

    private YearMonth parseMonth(String raw) {

        String value = clean(raw);

        if (value.matches("[0-9]{6}")) {
            return YearMonth.parse(
                    value,
                    DateTimeFormatter.ofPattern("uuuuMM")
            );
        }

        if (value.matches("[0-9]{4}\\.[0-9]{2}")) {
            value = value.replace('.', '-');
        }

        if (!value.matches("[0-9]{4}-[0-9]{2}")) {
            throw new IllegalArgumentException(
                    "무역 CSV 월 형식이 올바르지 않습니다."
            );
        }

        return YearMonth.parse(value);
    }

    private BigDecimal amount(String raw) {

        String value = raw.trim().replace(",", "");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "무역금액이 비어 있습니다."
            );
        }

        BigDecimal number = new BigDecimal(value);

        if (number.signum() < 0) {
            throw new IllegalArgumentException(
                    "수출입 금액이 음수입니다."
            );
        }

        return number;
    }

    private String clean(String value) {
        return value.replace("\uFEFF", "").trim();
    }
}
