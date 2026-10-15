
package main_project.matching.dto;

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

    // 수출입기업 정보
    private String shipperCompanyName;

    private String shipperContactName;

    private String shipperBizNumber;

    private String shipperPhone;

    private String shipperAddress;


    // 물류기업 정보
    private String logisticsCompanyName;

    private String logisticsContactName;

    private String logisticsBizNumber;

    private String logisticsPhone;

    private String logisticsAddress;


    // [추가] 운송 서비스 정보 (물류기업 매칭 조건에서 가져옴)

    // 운송 방식 : true = 해상, false = 항공
    private Boolean logisticsTransportType;

    // 정기노선 여부
    private Boolean logisticsRegularRoute;

    // 직항 여부
    private Boolean logisticsDirectRoute;

    // 해당 HS코드 취급 경험(회)
    private Integer logisticsExperienceCount;

    // 평균 운송 기간(일)
    private Integer logisticsAverageTransitDays;


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
