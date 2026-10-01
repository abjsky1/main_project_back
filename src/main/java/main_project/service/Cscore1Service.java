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
import main_project.model.entity.MemberEntity;
import main_project.model.repository.Cscore1Repository;
import main_project.model.repository.Cscore2Repository;
import main_project.model.repository.Cscore3Repository;
import main_project.model.repository.MemberRepository;

@Service 
@RequiredArgsConstructor 
public class Cscore1Service {

    private final Cscore1Repository cscore1Repository;

    private final Cscore2Repository cscore2Repository;

    private final Cscore3Repository cscore3Repository;

    private final MemberRepository memberRepository;

    // [1] 화주 매칭 조건 등록

    @Transactional 
    public boolean cscoreWrite( 
        Cscore1Dto cscore1Dto ,
        Cscore2Dto cscore2Dto , 
        Cscore3Dto cscore3Dto ) {

            // 1. 회원 조회
            MemberEntity memberEntity = memberRepository.findById(cscore1Dto.getMemberId())
                                        .orElse(null);
            
            if (memberEntity == null) {

                return false;
                
            }

            // 2. Cscore1 저장
            Cscore1Entity cscore1Entity = cscore1Dto.toEntity(memberEntity);

            Cscore1Entity savedCscore1 = cscore1Repository.save(cscore1Entity);

            if ( savedCscore1.getCscore1Id() == null ) {

                return false;

            }

            // 3. Cscore2 저장
            Cscore2Entity cscore2Entity = cscore2Dto.toEntity(savedCscore1);

            Cscore2Entity savedCscore2 = cscore2Repository.save(cscore2Entity);

            // 4. Cscore3 저장
            Cscore3Entity cscore3Entity = cscore3Dto.toEntity(savedCscore1);

            Cscore3Entity savedCscore3 = cscore3Repository.save(cscore3Entity);

            // 5. 저장 확인
            if (savedCscore2.getCscore2Id() != null && savedCscore3.getCscore3Id() != null ) {
                return true;
            }

        return false;

    }

    // [2] Cscore1 전체 조회
    public List<Cscore1Dto> cscoreRead(){

        List<Cscore1Entity> cscore1Entities = cscore1Repository.findAll();

        List<Cscore1Dto> cscore1Dtos = new ArrayList<>();

        cscore1Entities.forEach((cscore1Entity) -> {

            Cscore1Dto cscore1Dto = Cscore1Dto.from(cscore1Entity);

            cscore1Dtos.add(cscore1Dto);

        });

        return cscore1Dtos;

    }

    // [3] Cscore1 개별 조회
    public Cscore1Dto cscoreFindByID( Integer cscore1Id ) {

        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        if (cscore1Entity == null) {

            return null;
            
        }

        return Cscore1Dto.from(cscore1Entity);

    }

    // [4] Cscore1에 연결된 Cscore2 조회
    public Cscore2Dto cscore2Read( Integer cscore1Id ) {

        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        if (cscore1Entity == null) {

            return null;

        }

        Cscore2Entity cscore2Entity = cscore2Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        if (cscore2Entity == null) {

            return null;
            
        }

        return Cscore2Dto.from(cscore2Entity);

    }

    // [5] Cscore1에 연결된 Cscore3 조회
    public Cscore3Dto cscore3Read( Integer cscore1Id ) {

        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        if (cscore1Entity == null) {

            return null;

        }

        Cscore3Entity cscore3Entity = cscore3Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        if (cscore3Entity == null) {

            return null;
            
        }

        return Cscore3Dto.from(cscore3Entity);

    }

    // [6] 화주 매칭 조건 삭제
    @Transactional
    public boolean cscoreDelete(Integer cscore1Id) {

        // 1. Cscore1 조회
        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        if (cscore1Entity == null) {

            return false;

        }

        // 2. 연결된 Cscore2 조회
        Cscore2Entity cscore2Entity = cscore2Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 3. 연결된 Cscore3 조회
        Cscore3Entity cscore3Entity =cscore3Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 4. Cscore2 삭제
        if (cscore2Entity != null) {

            cscore2Repository.deleteById(cscore2Entity.getCscore2Id());

        }

        // 5. Cscore3 삭제
        if (cscore3Entity != null) {

            cscore3Repository.deleteById(cscore3Entity.getCscore3Id());

        }

        // 6. Cscore1 삭제
        cscore1Repository.deleteById(cscore1Id);

        return true;
    }

}
