package main_project.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.LoginDto;
import main_project.model.entity.MemberEntity;
import main_project.service.SignupService;

@RestController 
@RequestMapping ("/api/signup")
@RequiredArgsConstructor 
public class SignupController {

    private final SignupService signupService;

    @PostMapping ("/login")
    public boolean login(@RequestBody LoginDto loginDto) {
        MemberEntity member = signupService.login(loginDto.getUserEmail(),loginDto.getUserPassword());
        if (member != null){
        return true;
        } return false;
    }
    







}
