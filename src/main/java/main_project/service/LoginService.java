package main_project.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.LoginDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class LoginService {

    private final MemberRepository memberRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberEntity login(LoginDto loginDto) {
       
        // 1. 입력한 이메일과 일치하는 회원을 조회
       MemberEntity memberEntity = memberRepository.findByUserEmail(loginDto.getUserEmail()).orElse(null);

       if(memberEntity == null) {return null;}

       boolean passwordMatch = passwordEncoder.matches(loginDto.getUserPassword(), memberEntity.getUserPassword());

       if(passwordMatch == false) { return null;}

       if(!memberEntity.getUserPassword().equals(loginDto.getUserPassword())) { return null;}

       if (memberEntity.getStatus() == false) { return null;}
        return memberEntity;
    }

    }

