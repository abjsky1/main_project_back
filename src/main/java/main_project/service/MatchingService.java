package main_project.service;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore2Entity;
import main_project.model.entity.Cscore3Entity;
import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.Lscore2Entity;
import main_project.model.entity.Lscore3Entity;
import main_project.model.entity.MatchingEntity;
import main_project.model.repository.Cscore1Repository;
import main_project.model.repository.Cscore2Repository;
import main_project.model.repository.Cscore3Repository;
import main_project.model.repository.Lscore1Repository;
import main_project.model.repository.Lscore2Repository;
import main_project.model.repository.Lscore3Repository;
import main_project.model.repository.MatchingRepository;

@Service 
@RequiredArgsConstructor 
@Transactional
public class MatchingService {

    private final MatchingRepository matchingRepository;

    private final Cscore1Repository cscore1Repository;

    private final Cscore2Repository cscore2Repository;

    private final Cscore3Repository cscore3Repository;

    private final Lscore1Repository lscore1Repository;

    private final Lscore2Repository lscore2Repository;

    private final Lscore3Repository lscore3Repository;

    // [1] 수출입기업(화주)과 물류기업 1차 매칭 조건 확인
    public boolean matchingCheck( Integer cscore1Id , Integer lscore1Id ) {

        // 1. 수출입기업(화주) 기본 조건 조회
        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        if (cscore1Entity == null) {

            return false;
            
        }

        // 2. 수출입기업(화주)의 요청 물량 및 희망 날짜 조건 조회
        Cscore2Entity cscore2Entity = cscore2Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 3. 수출입기업(화주)의 특수화물 조건 조회
        Cscore3Entity cscore3Entity = cscore3Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 필요 조건 하나라도 불일치 시 탈락 ( 또는 || )
        if (cscore2Entity == null || cscore3Entity == null) {

            return false;

        }

        // 4. 물류업체 기본 조건 조회
        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        if (lscore1Entity == null) {

            return false;
            
        }

        // 5. 물류업체의 가용 물량 및 가능 날짜 조건 조회
        Lscore2Entity lscore2Entity = lscore2Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        // 6. 물류업체 특수화물 취급 조건 조회
        Lscore3Entity lscore3Entity = lscore3Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        // 필요 조건 하나라도 불일치 시 탈락
        if (lscore2Entity == null || lscore3Entity == null) {

            return false;
            
        }

        // 7. 조회한 데이터를 1차 필터에 전달
        return firstFilter(
            cscore1Entity, cscore2Entity, 
            cscore3Entity, lscore1Entity, 
            lscore2Entity, lscore3Entity);

    }

    // [2] 1차 필터 (필수조건 확인)
    private boolean firstFilter( 
        Cscore1Entity cscore1Entity ,
        Cscore2Entity cscore2Entity ,
        Cscore3Entity cscore3Entity , 
        Lscore1Entity lscore1Entity , 
        Lscore2Entity lscore2Entity ,
        Lscore3Entity lscore3Entity ) {

            // 1. 출발지 일치 여부 
            if (!cscore1Entity.getDeparture().equals(lscore1Entity.getDeparture())) {

                return false;

            }

            // 2. 도착지 일치 여부
            if (!cscore1Entity.getArrival().equals(lscore1Entity.getArrival())) {

                return false;
                
            }


            // 3. 운송방식 일치 여부
            if (!cscore1Entity.getTransportType().equals(lscore1Entity.getTransportType())) {
                
                return false;

            }

            // 4. 물류업체 가용 물량이 수출입기업 요청 물량 미만일 시 탈락
            if (lscore2Entity.getAvailableCapacity() < cscore2Entity.getRequestWeight()) {
                
                return false;

            }

            // 5. 수출입기업이 냉장/냉동 운송 필요로 하지만 물류업체가 냉장 불가능하면 탈락
            if (cscore3Entity.getRefrigerated() && !lscore3Entity.getRefrigerated()) {

                return false;
                
            }

            // 6. 수출입기업이 위험물 운송 필요로 하지만 물류업체가 취급 불가능하면 탈락
            if (cscore3Entity.getDangerous() && !lscore3Entity.getDangerous()) {
                
                return false;

            }

            // 7. 수출입기업이 중량물 운송 필요로 하지만 물류업체가 취급 불가능하면 탈락
            if (cscore3Entity.getHeavyCargo() && !lscore3Entity.getHeavyCargo()) {
                
                return false;

            }

            // 8. 수출입기업이 특수화물 운송 필요로 하지만 물류업체가 취급 불가능하면 탈락
            if (cscore3Entity.getSpecialCargo() && !lscore3Entity.getSpecialCargo()) {

                return false;
                
            }

        // 위 조건 해당하지 않을 시 1차 조건 통과
        return true;

    }

    // [3] 2치 필터 (매칭 점수 계산)

    // 1. 루트(노선) 점수 계산 : 30점
    private int routeScore(Lscore1Entity lscore1Entity) {

        // 정기노선 O + 직항 O : 30점
        if (lscore1Entity.getRegularRoute() && lscore1Entity.getDirectRoute()) {

            return 30;
            
        }

        // 정기노선 O + 직항 X : 25점
        if (lscore1Entity.getRegularRoute() && !lscore1Entity.getDirectRoute()) {

            return 25;
            
        }

        // 정기노선 X + 직항 O : 22점
        if (!lscore1Entity.getRegularRoute() && lscore1Entity.getDirectRoute()) {

            return 22;
            
        }
        
        // 정기노선 X + 직항 X : 18점
        return 18;

    }

    // 2. 가용 물량 점수 계산 (가용량 ÷ 요청량 × 100)
    private Integer capacityScore(
        Cscore2Entity cscore2Entity ,
        Lscore2Entity lscore2Entity) {

            double capacityRatio = lscore2Entity.getAvailableCapacity() / cscore2Entity.getRequestWeight() * 100;

            // 150% 이상 : 25점
            if (capacityRatio >= 150) {
                
                return 25;

            }

            // 120% 이상 : 23점
            if (capacityRatio >= 120) {
                
                return 23;

            }

            // 105% 이상 : 20점
            if (capacityRatio >= 105) {

                return 20;
                
            }

            // 100% 이상 : 15점
            if (capacityRatio >= 100) {

                return 15;
                
            }

            // 100% 미만은 원래 1차 필터에서 탈락
            return 0;

    }

    // 3. HS CODE 품목 적합도 점수 계산
    private Integer itemScore(
        Cscore1Entity cscore1Entity ,
        Lscore1Entity lscore1Entity) {

            // 수출입기업(화주) HS CODE에서 숫자만 남김
            String cHsCode = cscore1Entity.getHsCode().replaceAll("[^0-9]", "");

            // 물류기업 HS CODE에서 숫자만 남김
            String lHsCode = lscore1Entity.getHsCode().replaceAll("[^0-9]", "");

            // 10자리 전체 일치 : 20점
            if (cHsCode.equals(lHsCode)) {
                
                return 20;

            }

            // 앞 8자리 일치 : 17점
            if (cHsCode.substring(0, 8).equals(lHsCode.substring(0, 8))) {

                return 17;
                
            }

            // 앞 6자리 일치 : 14점
            if (cHsCode.substring(0, 6).equals(lHsCode.substring(0, 6))) {
                
                return 14;

            }

            // 앞 4자리 일치 : 12점
            if (cHsCode.substring(0, 4).equals(lHsCode.substring(0, 4))) {

                return 12;
                
            }

            // 앞 2자리 일치 : 8점
            if (cHsCode.substring(0, 2).equals(lHsCode.substring(0, 2))) {
                
                return 8;

            }

        // 無일치 : 0점
        return 0;

    }

    // 4. 일정 적합도 점수 계산 : 15점
    private Integer scheduleScore(
        Cscore2Entity cscore2Entity , 
        Lscore2Entity lscore2Entity) {

        // 날짜 일(day) 로 변환 
        // toEpochDay() : LocalDate 객체의 날짜를 경과한 날짜 수(long 타입)로 변환
        Long desiredDate = cscore2Entity.getDesiredDate().toEpochDay();

        Long availableDate = lscore2Entity.getAvailableDate().toEpochDay();

        Long dateDifference;

        // 희망 날짜가 더 크면 : 희망 날짜 - 가능 날짜
        if (desiredDate > availableDate) {

            dateDifference = desiredDate - availableDate;
            
        } else {

            // 가능 날짜가 더 크거나 같으면 :  같은 날짜 - 희망 날짜
            dateDifference = availableDate - desiredDate;

        }

        // 날짜가 같으면 : 15점
        if (dateDifference == 0) {

            return 15;
            
        }

        // 날짜 차이가 3일 이내 : 13점
        if (dateDifference <= 3) {
            
            return 13;

        }

        // 날짜 차이가 7일 이내 : 10점
        if (dateDifference <= 7) {
            
            return 10;

        }

        // 날짜 차이가 14일 이내 : 7점
        if (dateDifference <= 14) {
            
            return 7;

        }

        // 14일 초과지만 같은 연도 및 월 : 5점
        if (cscore2Entity.getDesiredDate().getYear() == lscore2Entity.getAvailableDate().getYear()
        && cscore2Entity.getDesiredDate().getMonth() == lscore2Entity.getAvailableDate().getMonth()) {

            return 5;
            
        }

        // 그 외 : 0점
        return 0;

    }

    // 5. 운송 경험에 따른 점수 계산 : 10점
    private Integer experienceScore(Lscore1Entity lscore1Entity) {

        // 물류업체의 운송 경험 횟수
        Integer experienceCount = lscore1Entity.getExperienceCount();

        // 800회 이상 : 10점
        if(experienceCount >= 800) {
            
            return 10;

        }

        // 600 ~ 799회 : 8점
        if (experienceCount >= 600) {
            
            return 8;

        }

        // 400 ~ 599회 : 6점
        if (experienceCount >= 400) {
            
            return 6;

        }

        // 200 ~ 399회 : 4점
        if (experienceCount >= 200) {
            
            return 4;

        }

        // 1 ~ 199회
        if (experienceCount >= 1) {

            return 2;
            
        }

    return 0;

    }

    // [4] 최종 매칭 실행
    @Transactional 
    public boolean matchingWrite(Integer cscore1Id) {

        // 1. 수출입기업(화주) 기본 조건 조회
        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        // 해당 화주 조건이 존재하지 않으면 매칭 실패
        if (cscore1Entity == null) {
            
            return false;

        }

        // 2. Cscore1에 연결된 수출입기업(화주) 물량/일정 조회
        Cscore2Entity cscore2Entity = cscore2Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 3. Cscore1에 연결된수출입기업(화주) 특수화물 조건 조회
        Cscore3Entity cscore3Entity = cscore3Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // Cscore2 또는 Cscore3 없으면 매칭에 필요한 정보가 부족하므로 실패
        if (cscore2Entity == null || cscore3Entity == null) {
            
            return false;

        }

        // 4. 이미 매칭된 수출입기업(화주) 조건인지 확인
        List<MatchingEntity> matchingEntities = matchingRepository.findAll();

        // 기존 매칭 데이터를 하나씩 확인
        for(MatchingEntity matchingEntity : matchingEntities) {

            // 같은 cscore1Id가 이미 Matching에 존재하면 중복 매칭을 하지 않고 false 반환
            if (matchingEntity.getCscore1Entity().getCscore1Id().equals(cscore1Id)) {

                return false;
                
            }
        }

        // 5. 하나씩 비교하기 위해 등록된 모든 물류업체 조건 전체 조회
        List<Lscore1Entity> lscore1Entities = lscore1Repository.findAll();

        // 6. 최고점 업체와 점수를 저장할 변수, 업체 선택 안 한 상태 = null
        Lscore1Entity topLscore1Entity = null;

        // 최고점 업체의 각 점수를 저장할 변수
        Integer topRouteScore = 0;

        Integer topCapacityScore = 0;

        Integer topItemScore = 0;

        Integer topScheduleScore = 0;

        Integer topExperienceScore = 0;

        // 현재까지 가장 높은 총점, 처음에는 어떤 업체도 계산하지 않았으므로 -1로 시작
        Integer topTotalScore = -1;

        // 7. 등록된 물류기업을 하나씩 반복 비교
        for(Lscore1Entity lscore1Entity : lscore1Entities) {

            // 현재 물류업체 가용 물량 및 가능 날짜 등 조건 조회
            Lscore2Entity lscore2Entity = lscore2Repository.findByLscore1Entity(lscore1Entity).orElse(null);

            // 현재 물류업체 냉장, 위험물, 중량물 등 조건 조회
            Lscore3Entity lscore3Entity = lscore3Repository.findByLscore1Entity(lscore1Entity).orElse(null);

            // Lscore2 또는 Lscore3 없으면 비교 불가, 다음 물류기업으로 이동
            if (lscore2Entity == null || lscore3Entity == null) {
                
                continue;

            }

            // 8. 화주 조건과 현재 물류업체 조건을 조건을 1차 필터로 검사
            boolean filterResult = firstFilter(cscore1Entity, cscore2Entity, cscore3Entity, lscore1Entity, lscore2Entity, lscore3Entity);

            // 1차 필수조건에서 탈락한 업체 -> 점수 계산하지 않고 다음 업체
            if (filterResult == false) {
                
                continue;

            }

            // 9. 1차 필터를 통과한 업체의 2차 점수 계산

            // 노선 점수 계산
            Integer routeScore = routeScore(lscore1Entity);

            // 가용 물량 점수 계산
            Integer capacityScore = capacityScore(cscore2Entity, lscore2Entity);

            // 품목 적합도 점수 계산
            Integer itemScore = itemScore(cscore1Entity, lscore1Entity);

            // 일정 적합도 점수 계산
            Integer scheduleScore = scheduleScore(cscore2Entity, lscore2Entity);

            // 운송 경험 점수 계산
            Integer experienceScore = experienceScore(lscore1Entity);

            // 10. 계산한 5개 점수 합한 총점 계산
            Integer totalscore = routeScore + capacityScore + itemScore + scheduleScore + experienceScore;

            // 11. 현재 업체의 총점이 기존 최고점보다 높은지 확인
            if (totalscore > topTotalScore) {

                // 최고점 물류업체를 현재 업체로 변경
                topLscore1Entity = lscore1Entity;

                // 현재 업체의 각 점수도 최고점 정보로 저장
                topRouteScore = routeScore;

                topCapacityScore = capacityScore;

                topItemScore = itemScore;

                topScheduleScore = scheduleScore;

                topExperienceScore = experienceScore;

                // 현재 업체의 총점을 새로운 최고점으로 저장
                topTotalScore = totalscore;
                
            }

        }

        // 12. 모든 물류업체 확인 후 1차 필터 통과가 존재하지 않으면 매칭 실패
        if (topLscore1Entity == null) {
            
            return false;

        }

        // 13. 최종 매칭된 수출입기업과 물류업체 정보를 MatchingEntity로 생성
        MatchingEntity matchingEntity = MatchingEntity.builder()
                                        .cscore1Entity(cscore1Entity)
                                        .lscore1Entity(topLscore1Entity)
                                        .routeScore(topRouteScore)
                                        .capacityScore(topCapacityScore)
                                        .itemScore(topItemScore)
                                        .scheduleScore(topScheduleScore)
                                        .experienceScore(topExperienceScore)
                                        .totalScore(topTotalScore)
                                        .build();

        // 14. 매칭 결과를 테이블에 저장
        MatchingEntity savedMatchingEntity = matchingRepository.save(matchingEntity);

        // 15. 저장 후 matchingId가 생성되면 정상 저장
        if (savedMatchingEntity.getMatchingId() != null) {
            

            return true;

        }

        return false;

    }

}
