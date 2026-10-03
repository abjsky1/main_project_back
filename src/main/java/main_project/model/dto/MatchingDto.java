package main_project.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
@Data 
public class MatchingDto {

    // 매칭 PK
    private Integer matchingId;

    // 수출입기업 조건 PK
    private Integer cscore1Id;

    // 물류기업 조건 PK
    private Integer lscore1Id;

    // 매칭 점수
    private Integer routeScore;

    private Integer capacityScore;

    private Integer itemScore;

    private Integer scheduleScore;

    private Integer experienceScore;

    private Integer totalScore;

    // 관리자 검토 상태
    // PENDING / APPROVED / REJECTED
    private String adminStatus;

    // 수출입기업 응답 상태
    // WAITING / ACCEPTED / REJECTED
    private String shipperStatus;

    // 물류기업 응답 상태
    // WAITING / ACCEPTED / REJECTED
    private String logisticsStatus;

    // 최종 매칭 상태
    // PENDING / COMPLETED / FAILED
    private String finalStatus;

    // 추천 이유
    private String recommendReason;

    // 경고 메시지
    private String warningMessage;

    // 매칭 생성일
    private LocalDateTime createdAt;

}