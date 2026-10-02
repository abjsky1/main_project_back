package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore2Entity;
import main_project.model.entity.Cscore3Entity;
import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.Lscore2Entity;
import main_project.model.entity.Lscore3Entity;
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
        Cscore2Entity cscore2Entity = cscore2Repository.findById(cscore1Id).orElse(null);

        // 3. 수출입기업(화주)의 특수화물 조건 조회
        Cscore3Entity cscore3Entity = cscore3Repository.findById(cscore1Id).orElse(null);

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
        Lscore2Entity lscore2Entity = lscore2Repository.findById(lscore1Id).orElse(null);

        // 6. 물류업체 특수화물 취급 조건 조회
        Lscore3Entity lscore3Entity = lscore3Repository.findById(lscore1Id).orElse(null);

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


}
