package main_project.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.auth.dto.LoginDto;
import main_project.member.dto.MemberDto;
import main_project.member.entity.MemberEntity;
import main_project.auth.service.SignupService;

@RestController 
@RequestMapping ("/api/auth/signup")
@RequiredArgsConstructor 
public class SignupController {

    private final SignupService signupService;

     // 회원가입
    @PostMapping
    public boolean signup(@RequestBody MemberDto memberDto) {

        return signupService.signup(memberDto);
    
    }



}
