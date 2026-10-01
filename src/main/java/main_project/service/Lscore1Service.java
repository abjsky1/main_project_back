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
import main_project.model.entity.Lscore2Entity;
import main_project.model.entity.Lscore3Entity;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.Lscore1Repository;
import main_project.model.repository.Lscore2Repository;
import main_project.model.repository.Lscore3Repository;
import main_project.model.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class Lscore1Service {

    private final Lscore1Repository lscore1Repository;

    private final Lscore2Repository lscore2Repository;

    private final Lscore3Repository lscore3Repository;

    private final MemberRepository memberRepository;


    // [1] 물류업체 매칭 조건 등록
    @Transactional
    public boolean lscoreWrite(
            Lscore1Dto lscore1Dto,
            Lscore2Dto lscore2Dto,
            Lscore3Dto lscore3Dto) {

        // 1. 회원 조회
        MemberEntity memberEntity = memberRepository.findById(lscore1Dto.getMemberId()).orElse(null);

        if (memberEntity == null) {

            return false;

        }


        // 2. Lscore1 저장
        Lscore1Entity lscore1Entity = lscore1Dto.toEntity(memberEntity);

        Lscore1Entity savedLscore1 = lscore1Repository.save(lscore1Entity);

        if (savedLscore1.getLscore1Id() == null) {

            return false;

        }


        // 3. Lscore2 저장
        Lscore2Entity lscore2Entity = lscore2Dto.toEntity(savedLscore1);

        Lscore2Entity savedLscore2 = lscore2Repository.save(lscore2Entity);


        // 4. Lscore3 저장
        Lscore3Entity lscore3Entity = lscore3Dto.toEntity(savedLscore1);

        Lscore3Entity savedLscore3 = lscore3Repository.save(lscore3Entity);


        // 5. 저장 확인
        if (savedLscore2.getLscore2Id() != null &&
            savedLscore3.getLscore3Id() != null) {

            return true;

        }

        return false;

    }


    // [2] Lscore1 전체 조회
    public List<Lscore1Dto> lscoreRead() {

        List<Lscore1Entity> lscore1Entities = lscore1Repository.findAll();

        List<Lscore1Dto> lscore1Dtos = new ArrayList<>();

        lscore1Entities.forEach((lscore1Entity) -> {Lscore1Dto lscore1Dto = Lscore1Dto.from(lscore1Entity);

            lscore1Dtos.add(lscore1Dto);

        });

        return lscore1Dtos;

    }


    // [3] Lscore1 개별 조회
    public Lscore1Dto lscoreFindById(Integer lscore1Id) {

        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        if (lscore1Entity == null) {

            return null;

        }

        return Lscore1Dto.from(lscore1Entity);
    }


    // [4] Lscore1에 연결된 Lscore2 조회
    public Lscore2Dto lscore2Read(Integer lscore1Id) {

        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        if (lscore1Entity == null) {

            return null;

        }

        Lscore2Entity lscore2Entity = lscore2Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        if (lscore2Entity == null) {

            return null;

        }

        return Lscore2Dto.from(lscore2Entity);
    }


    // [5] Lscore1에 연결된 Lscore3 조회
    public Lscore3Dto lscore3Read(Integer lscore1Id) {

        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        if (lscore1Entity == null) {

            return null;

        }

        Lscore3Entity lscore3Entity = lscore3Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        if (lscore3Entity == null) {

            return null;

        }

        return Lscore3Dto.from(lscore3Entity);

    }


    // [6] 물류업체 매칭 조건 삭제
    @Transactional
    public boolean lscoreDelete(Integer lscore1Id) {

        // 1. Lscore1 조회
        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        if (lscore1Entity == null) {

            return false;

        }


        // 2. 연결된 Lscore2 조회
        Lscore2Entity lscore2Entity = lscore2Repository.findByLscore1Entity(lscore1Entity).orElse(null);


        // 3. 연결된 Lscore3 조회
        Lscore3Entity lscore3Entity = lscore3Repository.findByLscore1Entity(lscore1Entity).orElse(null);


        // 4. Lscore2 삭제
        if (lscore2Entity != null) {

            lscore2Repository.deleteById(lscore2Entity.getLscore2Id());

        }


        // 5. Lscore3 삭제
        if (lscore3Entity != null) {lscore3Repository.deleteById(lscore3Entity.getLscore3Id());

        }


        // 6. 마지막으로 Lscore1 삭제
        lscore1Repository.deleteById(lscore1Id);

        return true;
        
    }

}