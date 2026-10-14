package main_project.model.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
@Data 
public class MatchingAiDto {

    // [1] AI 평가 상태
    private String evaluationStatus;

    // [2] 항목별 점수
    private Integer routeScore;       // 노선 30점
    private Integer capacityScore;    // 가용 물량 25점
    private Integer itemScore;        // HS코드 20점
    private Integer scheduleScore;    // 일정 15점
    private Integer experienceScore;  // 운송 경험 10점

    // [3] 총점
    private Integer totalScore;

    // [4] 추천 이유와 주의사항
    private String recommendReason;
    private String warningMessage;

    // [5] 항목별 상세 평가 근거
    private Map<String, ScoreReason> scoreReasons;

    // [6] 누락된 데이터 목록
    private List<String> missingFields;

    @Data 
    public static class ScoreReason {

        // 점수 계산 과정
        private String calculation;

        // 점수 부여 이유
        private String reason;

        // 참고 문헌 ID
        private List<String> evidenceIds;
    }

}
