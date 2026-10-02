package main_project.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.LoginDto;
import main_project.model.entity.MemberEntity;
import main_project.service.LoginService;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @PostMapping
    public MemberEntity login(@RequestBody LoginDto loginDto) {
        
        return loginService.login(loginDto);
    }
}