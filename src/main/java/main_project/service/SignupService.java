package main_project.service;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;
import main_project.model.repository.SignupRepository;

@Service 
@RequiredArgsConstructor
@Transactional 
public class SignupService {

    private final SignupRepository signupRepository;

    private final MemberRepository memberRepository;


    public MemberEntity login(String userEmail, String userPassword) {
        // 회원 전체 조회
        List<MemberEntity> memberList = memberRepository.findAll();

        for ( MemberEntity member : memberList) {

            if( member.getUserEmail().equals(userEmail)&&
                member.getUserPassword().equals(userPassword)&&
                member.getStatus() == true ) 
                return member;   
        }
        return null;
    }
    


}
