package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.Cscore2Service;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class Cscore2Controller {

    private final Cscore2Service cscore2Service;





}
