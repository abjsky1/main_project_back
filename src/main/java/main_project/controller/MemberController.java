package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.MemberService;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class MemberController {

    private final MemberService memberService;






}
