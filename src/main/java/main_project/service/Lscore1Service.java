package main_project.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.Lscore1Dto;
import main_project.model.dto.Lscore2Dto;
import main_project.model.dto.Lscore3Dto;
import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Lscore2Entity;
import main_project.model.entity.Lscore3Entity;
import main_project.model.entity.MatchingEntity;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.Lscore1Repository;
import main_project.model.repository.Lscore2Repository;
import main_project.model.repository.Lscore3Repository;
import main_project.model.repository.MatchingRepository;
import main_project.model.repository.MemberRepository;
import main_project.model.repository.Cscore1Repository;

@Service
@RequiredArgsConstructor
public class Lscore1Service {

    private final Lscore1Repository lscore1Repository;

    private final Lscore2Repository lscore2Repository;

    private final Lscore3Repository lscore3Repository;

    private final MemberRepository memberRepository;

    private final MatchingRepository matchingRepository;

    private final MatchingService matchingService;
    private final Cscore1Repository cscore1Repository;

    // [1] 물류업체 매칭 조건 등록
    @Transactional
    public boolean lscoreWrite(
            Lscore1Dto lscore1Dto,
            Lscore2Dto lscore2Dto,
            Lscore3Dto lscore3Dto) {

        // 1. 전달받은 memberId로 실제 회원 조회
        MemberEntity memberEntity = memberRepository.findById(lscore1Dto.getMemberId()).orElse(null);

        // 회원이 없으면 등록 실패
        if (memberEntity == null) {

            return false;

        }


        // [추가] 비활성 물류기업 회원은 매칭 조건 등록 불가
        if (memberEntity.getStatus() == null || memberEntity.getStatus() == false) {

            return false;

        }

        // 2. 물류업체의 기본 매칭 조건(Lscore1) 저장
        Lscore1Entity lscore1Entity = lscore1Dto.toEntity(memberEntity);

        Lscore1Entity savedLscore1 = lscore1Repository.save(lscore1Entity);

        // Lscore1 저장 실패 시 false 반환
        if (savedLscore1.getLscore1Id() == null) {

            return false;

        }


        // 3. 가용 물량, 가능 날짜 등의 조건(Lscore2) 저장, savedLscore1과 연결해서 저장
        Lscore2Entity lscore2Entity = lscore2Dto.toEntity(savedLscore1);

        Lscore2Entity savedLscore2 = lscore2Repository.save(lscore2Entity);


        // 4. 냉장, 위험물, 중량물, 특수화물 등의 조건(Lscore3) 저장, savedLscore1과 연결해서 저장
        Lscore3Entity lscore3Entity = lscore3Dto.toEntity(savedLscore1);

        Lscore3Entity savedLscore3 = lscore3Repository.save(lscore3Entity);


        // 5. Lscore2와 Lscore3가 모두 정상 저장되었는지 확인
        if (savedLscore2.getLscore2Id() != null &&
            savedLscore3.getLscore3Id() != null) {

            // 화주가 먼저 조건을 등록했어도 신규 물류기업 등록 시 추천 후보를 다시 찾음
            if (savedLscore1.getMatchingAgree() != null && savedLscore1.getMatchingAgree() == true) {
                for (Cscore1Entity c : cscore1Repository.findByMatchingAgreeTrue()) {
                    // 활성 상태의 화주만 자동 매칭 재검사
                    if (c.getMemberEntity().getStatus() != null
                            && c.getMemberEntity().getStatus() == true) {
                        matchingService.matchingWrite(c.getCscore1Id());
                    }
                }
            }
            return true;

        }

        // 등록 실패
        return false;

    }


    // [2] Lscore1 조회
    public List<Lscore1Dto> lscoreRead(String memberId) {

        // 1. 회원 ID가 있으면 해당 회원의 조건만 조회
        List<Lscore1Entity> lscore1Entities = memberId == null
                ? lscore1Repository.findAll()
                : lscore1Repository.findByMemberEntityMemberId(memberId);

        // 2. Entity를 DTO로 변환해서 담을 리스트 생성
        List<Lscore1Dto> lscore1Dtos = new ArrayList<>();

        // 3. 조회된 Entity를 하나씩 DTO로 변환
        lscore1Entities.forEach((lscore1Entity) -> {Lscore1Dto lscore1Dto = Lscore1Dto.from(lscore1Entity);

            // 변환한 DTO를 리스트에 추가
            lscore1Dtos.add(lscore1Dto);

        });

        // 4. 전체 Lscore1 DTO 리스트 반환
        return lscore1Dtos;

    }


    // [3] Lscore1 개별 조회
    public Lscore1Dto lscoreFindById(Integer lscore1Id) {

        // 1. 전달받은 lscore1Id로 특정 물류업체 조건 조회
        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        // 해당 데이터가 없으면 null 반환
        if (lscore1Entity == null) {

            return null;

        }

        // 2. 조회한 Entity를 DTO로 변환해서 반환
        return Lscore1Dto.from(lscore1Entity);
    }


    // [4] Lscore1에 연결된 Lscore2 조회
    public Lscore2Dto lscore2Read(Integer lscore1Id) {

        // 1. 먼저 lscore1Id로 Lscore1 조회
        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        // Lscore1이 없으면 null 반환
        if (lscore1Entity == null) {

            return null;

        }

        // 2. 해당 Lscore1과 연결된 Lscore2 조회 및 가용 물량, 가능 날짜 등의 정보
        Lscore2Entity lscore2Entity = lscore2Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        // 연결된 Lscore2가 없으면 null 반환
        if (lscore2Entity == null) {

            return null;

        }

        // 3. 조회한 Entity를 DTO로 변환해서 반환
        return Lscore2Dto.from(lscore2Entity);
    }


    // [5] Lscore1에 연결된 Lscore3 조회
    public Lscore3Dto lscore3Read(Integer lscore1Id) {

        // 1. 먼저 lscore1Id로 Lscore1 조회
        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        // Lscore1이 없으면 null 반환
        if (lscore1Entity == null) {

            return null;

        }

        // 2. 해당 Lscore1과 연결된 Lscore3 조회 및 냉장, 위험물, 중량물, 특수화물 등의 정보
        Lscore3Entity lscore3Entity = lscore3Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        // 연결된 Lscore3가 없으면 null 반환
        if (lscore3Entity == null) {

            return null;

        }

        // 3. 조회한 Entity를 DTO로 변환해서 반환
        return Lscore3Dto.from(lscore3Entity);

    }


    // [6] 물류업체 매칭 조건 삭제
    @Transactional
    public boolean lscoreDelete(Integer lscore1Id) {

        // 1. 삭제할 Lscore1 조회
        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        // 해당 Lscore1이 존재하지 않으면 삭제 실패
        if (lscore1Entity == null) {

            return false;

        }

        // 2. 해당 물류기업 조건의 매칭 결과만 조회
        List<MatchingEntity> matchingEntities = matchingRepository.findByLscore1EntityLscore1Id(lscore1Id);

        // 3. 요청/완료/거절된 매칭은 삭제 불가, 자동 추천 후보만 삭제 가능
        for (MatchingEntity matchingEntity : matchingEntities) {

            if (!"PENDING".equals(matchingEntity.getFinalStatus())
                    || !"WAITING".equals(matchingEntity.getShipperStatus())
                    || !"WAITING".equals(matchingEntity.getLogisticsStatus())) {
                return false;
            }
        }

        // 4. 상태가 변경되지 않은 자동 추천 후보는 먼저 삭제
        for (MatchingEntity matchingEntity : matchingEntities) {
            matchingRepository.delete(matchingEntity);
        }

        matchingRepository.flush();

        // 4. Lscore1과 연결된 Lscore2 조회
        Lscore2Entity lscore2Entity = lscore2Repository.findByLscore1Entity(lscore1Entity).orElse(null);


        // 5. Lscore1과 연결된 Lscore3 조회
        Lscore3Entity lscore3Entity = lscore3Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        // 6. 연결된 Lscore2가 존재하면 삭제
        if (lscore2Entity != null) {

            lscore2Repository.deleteById(lscore2Entity.getLscore2Id());

        }

        // 7. 연결된 Lscore3가 존재하면 삭제
        if (lscore3Entity != null) {

            lscore3Repository.deleteById(lscore3Entity.getLscore3Id());

        }

        // 8. 마지막으로 Lscore1 삭제
        lscore1Repository.deleteById(lscore1Id);

        return true;

    }

}