package main_project.model.dto;

import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public final class InsightDto {

    private InsightDto() {
    }

    // 분석 요청
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private List<Integer> countryIds;
        private String hsCode;
        private String direction;
        private String start;
        private String end;
        private String objective;
        private Integer conditionId;
    }

    // 국가 선택지
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Country {
        private int id;
        private String code;
        private String name;
    }

    // 품목 선택지
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private String code;
        private String name;
    }

    // 회원의 저장된 업무 조건
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Condition {
        private int id;
        private int countryId;
        private String hsCode;
        private String direction;
        private String label;
        private Map<String, Object> details;
    }

    // DB에서 조회한 회원 유형과 업무 조건
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Profile {
        private String memberType;
        private List<Condition> conditions;
    }

    // 화면 초기 정보
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Options {
        private String memberType;
        private List<Condition> conditions;
        private List<Country> countries;
        private List<String> objectives;
        private String start;
        private String end;
        private String minMonth;
        private String maxMonth;
    }

    // CSV 월별 금액과 그래프 값에 공통 사용
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Point {
        private String period;
        private BigDecimal exportAmount;
        private BigDecimal importAmount;
        private BigDecimal balance;
    }

    // 국가별 그래프
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Series {
        private String country;
        private List<Point> points;
        private int observedMonths;
        private int expectedMonths;
    }

    // 지표
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Metric {
        private String id;
        private String country;
        private String name;
        private BigDecimal value;
        private String unit;
        private String period;
        private String source;
        private String completeness;
    }

    // 통계 변화
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Trend {
        private String id;
        private String country;
        private String title;
        private String period;
        private String source;
        private List<String> metricIds;
        private String description;
    }

    // 통계 계산 결과와 최종 분석 결과
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Result {

        private String memberType;
        private Request filters;
        private String itemName;
        private Instant generatedAt;
        private String aiStatus;

        // 이번 분석에 실제 사용한 DB 업무 정보
        private List<Condition> businessConditions;

        private List<Series> series;
        private List<Metric> metrics;
        private List<Trend> trends;

        private JsonNode narrative;
        private List<String> limitations;
    }

    // 분석 결과와 저장 여부
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Generation {
        private Integer historyId;
        private boolean saved;
        private String saveMessage;
        private Result result;
    }

    // 분석 당시 필터와 실제 사용한 업무 조건
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ConditionSnapshot {
        private String memberType;
        private Request filters;
        private List<Condition> businessConditions;
    }

    // 목록에 표시할 요약 정보
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HistoryItem {
        private Integer id;
        private Instant createdAt;
        private String status;
        private String itemName;
        private String hsCode;
        private String direction;
        private String start;
        private String end;
    }

    // 기록 목록과 페이지 정보
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HistoryPage {
        private List<HistoryItem> items;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    // 저장된 결과 전체 조회
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HistoryDetail {
        private Integer id;
        private Instant createdAt;
        private String status;
        private ConditionSnapshot condition;
        private Result result;
        private JsonNode metadata;
    }

    // 원본 파일 정보
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DataFile {
        private String name;
        private boolean exists;
        private Long size;
        private String modifiedAt;
    }

    // 국가 ID → 월 → 월별 수출입 값
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Dataset {
        private Map<Integer, Map<YearMonth, Point>> monthly;
        private List<String> observedItemNames;
        private List<DataFile> files;
        private List<String> limitations;
    }
}