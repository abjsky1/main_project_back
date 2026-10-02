package main_project.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.Cscore1Dto;
import main_project.model.dto.Cscore2Dto;
import main_project.model.dto.Cscore3Dto;
import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore2Entity;
import main_project.model.entity.Cscore3Entity;
import main_project.model.entity.MatchingEntity;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.Cscore1Repository;
import main_project.model.repository.Cscore2Repository;
import main_project.model.repository.Cscore3Repository;
import main_project.model.repository.MatchingRepository;
import main_project.model.repository.MemberRepository;

@Service 
@RequiredArgsConstructor 
public class Cscore1Service {

    private final Cscore1Repository cscore1Repository;

    private final Cscore2Repository cscore2Repository;

    private final Cscore3Repository cscore3Repository;

    private final MemberRepository memberRepository;

    private final MatchingRepository matchingRepository;

    // [1] 화주 매칭 조건 등록
    @Transactional 
    public boolean cscoreWrite( 
        Cscore1Dto cscore1Dto ,
        Cscore2Dto cscore2Dto , 
        Cscore3Dto cscore3Dto ) {

            // 1. Cscore1Dto에 전달받은 memberId를 이용해서 실제로 존재하는 회원인지 DB에서 조회
            MemberEntity memberEntity = memberRepository.findById(cscore1Dto.getMemberId())
                                        .orElse(null);
            
            // 회원이 존재하지 않으면 매칭 조건을 등록할 수 없으므로 false 반환
            if (memberEntity == null) {

                return false;
                
            }

            // 2. Cscore1Dto를 Cscore1Entity로 변환
            //    Cscore1은 회원, 국가, HS CODE, 운송방식, 출발지, 도착지 등의 화주 기본 매칭 조건을 저장
            Cscore1Entity cscore1Entity = cscore1Dto.toEntity(memberEntity);

            // 변환한 Cscore1Entity를 DB에 저장
            Cscore1Entity savedCscore1 = cscore1Repository.save(cscore1Entity);

            // 저장 후 PK인 cscore1Id가 생성되지 않았다면 저장 실패로 판단
            if ( savedCscore1.getCscore1Id() == null ) {

                return false;

            }

            // 3. Cscore2Dto를 Cscore2Entity로 변환
            //    Cscore2는 요청 물량과 희망 운송일 정보를 저장
            //    Cscore2는 Cscore1과 연결되어 있기 때문에 먼저 저장된 savedCscore1을 전달
            Cscore2Entity cscore2Entity = cscore2Dto.toEntity(savedCscore1);

            // Cscore2를 DB에 저장
            Cscore2Entity savedCscore2 = cscore2Repository.save(cscore2Entity);

            // 4. Cscore3Dto를 Cscore3Entity로 변환
            //    Cscore3는 냉장/냉동, 위험물, 중량물, 특수화물 등 화물의 특수 취급 조건을 저장, savedCscore1을 전달
            Cscore3Entity cscore3Entity = cscore3Dto.toEntity(savedCscore1);

            // Cscore3를 DB에 저장
            Cscore3Entity savedCscore3 = cscore3Repository.save(cscore3Entity);

            // 5. Cscore2와 Cscore3의 PK가 정상적으로 생성되었는지 확인
            //    둘 다 저장되었다면 전체 화주 매칭 조건 등록 성공
            if (savedCscore2.getCscore2Id() != null && savedCscore3.getCscore3Id() != null ) {
                return true;
            }

        // 등록 실패
        return false;

    }

    // [2] Cscore1 전체 조회
    public List<Cscore1Dto> cscoreRead(){

        // 1. Cscore1 테이블에 등록된 모든 데이터를 조회
        List<Cscore1Entity> cscore1Entities = cscore1Repository.findAll();

        // 2. 조회한 Entity를 DTO로 변환해서 담을 새로운 List를 생성
        List<Cscore1Dto> cscore1Dtos = new ArrayList<>();

        // 3. 조회된 Cscore1Entity를 하나씩 반복
        cscore1Entities.forEach((cscore1Entity) -> {

            // Entity 데이터를 Cscore1Dto로 변환
            Cscore1Dto cscore1Dto = Cscore1Dto.from(cscore1Entity);

            // 변환된 DTO를 반환용 List에 추가
            cscore1Dtos.add(cscore1Dto);

        });

        // 4. 모든 Cscore1 데이터를 DTO List 형태로 반환
        return cscore1Dtos;

    }

    // [3] Cscore1 개별 조회
    public Cscore1Dto cscoreFindByID( Integer cscore1Id ) {

        // 1. 전달받은 cscore1Id를 이용해서 해당 화주 매칭 조건을 DB에서 조회
        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        // 해당 번호의 Cscore1 데이터가 존재하지 않으면 null 반환
        if (cscore1Entity == null) {

            return null;
            
        }

        // 2. 조회한 Entity를 DTO로 변환해서 반환
        return Cscore1Dto.from(cscore1Entity);

    }

    // [4] Cscore1에 연결된 Cscore2 조회
    public Cscore2Dto cscore2Read( Integer cscore1Id ) {

        // 1. 먼저 전달받은 cscore1Id로 Cscore1을 조회
        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        // 해당 Cscore1이 존재하지 않으면 조회할 Cscore2도 없어 null 반환
        if (cscore1Entity == null) {

            return null;

        }

        // 2. 조회된 Cscore1Entity와 연결되어 있는 Cscore2를 조회
        //    Cscore2에는 요청 물량(requestWeight), 희망 운송일(desiredDate) 저장
        Cscore2Entity cscore2Entity = cscore2Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 연결된 Cscore2가 존재하지 않으면 null 반환
        if (cscore2Entity == null) {

            return null;
            
        }

        // 3. 조회한 Cscore2Entity를 DTO로 변환해서 반환
        return Cscore2Dto.from(cscore2Entity);

    }

    // [5] Cscore1에 연결된 Cscore3 조회
    public Cscore3Dto cscore3Read( Integer cscore1Id ) {

        // 1. 먼저 전달받은 cscore1Id로 Cscore1을 조회
        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        // 해당 Cscore1이 존재하지 않으면 조회할 Cscore3도 없으므로 null 반환
        if (cscore1Entity == null) {

            return null;

        }

        // 2. 조회된 Cscore1Entity와 연결되어 있는 Cscore3를 한다.
        //    Cscore3는 냉장/냉동, 위험물, 중량물, 특수화물 등의 취급 조건 저장
        Cscore3Entity cscore3Entity = cscore3Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 연결된 Cscore3가 존재하지 않으면 null 반환
        if (cscore3Entity == null) {

            return null;
            
        }

        // 3. 조회한 Cscore3Entity를 DTO로 변환해서 반환
        return Cscore3Dto.from(cscore3Entity);

    }

    // [6] 화주 매칭 조건 삭제
    @Transactional
    public boolean cscoreDelete(Integer cscore1Id) {

        // 1. Cscore1 조회
        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        if (cscore1Entity == null) { return false; }

        // 2. Matching 전체 조회
        List<MatchingEntity> matchingEntities = matchingRepository.findAll();

        // 3. 이미 매칭된 조건인지 확인
        for (MatchingEntity matchingEntity : matchingEntities) {

            if (matchingEntity.getCscore1Entity().getCscore1Id().equals(cscore1Id)) {

                // 이미 매칭된 조건이면 삭제 불가
                return false;

            }

        }

        // 4. 연결된 Cscore2 조회
        Cscore2Entity cscore2Entity = cscore2Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 5. 연결된 Cscore3 조회
        Cscore3Entity cscore3Entity = cscore3Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 6. Cscore2 삭제
        if (cscore2Entity != null) {

            cscore2Repository.deleteById(cscore2Entity.getCscore2Id());
            
        }

        // 7. Cscore3 삭제
        if (cscore3Entity != null) {

            cscore3Repository.deleteById(cscore3Entity.getCscore3Id());

        }

        // 8. Cscore1 삭제
        cscore1Repository.deleteById(cscore1Id);

        return true;

    }

}
