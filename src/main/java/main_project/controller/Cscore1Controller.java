package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.Cscore1Service;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class Cscore1Controller {

    private final Cscore1Service cscore1Service;





}
