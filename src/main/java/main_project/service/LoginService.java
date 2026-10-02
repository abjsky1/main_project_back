package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.LoginDto;
import main_project.model.entity.MemberEntity;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class LoginService {

    public MemberEntity login(LoginDto loginDto) {
        
        MemberEntity memberEntity = new MemberEntity();
        return memberEntity;
    }
    }

