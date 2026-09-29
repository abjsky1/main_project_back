package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.Lscore3Service;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class Lscore3Controller {

    private final Lscore3Service lscore3Service;





}
