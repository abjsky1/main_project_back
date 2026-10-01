package main_project.service;

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

    // 화주 매칭 조건 등록

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

    
    





    
}
