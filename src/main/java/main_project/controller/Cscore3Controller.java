package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.Cscore3Service;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class Cscore3Controller {

    private final Cscore3Service cscore3Service;





}
