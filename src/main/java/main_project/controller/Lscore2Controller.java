package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.Lscore2Service;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class Lscore2Controller {

    private final Lscore2Service lscore2Service;





}
