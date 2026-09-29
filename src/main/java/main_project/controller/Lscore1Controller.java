package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.Lscore1Service;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class Lscore1Controller {

    private final Lscore1Service lscore1Service;





}
