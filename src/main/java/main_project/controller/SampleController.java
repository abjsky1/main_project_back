package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.SampleService;

@RestController 
@RequiredArgsConstructor 
@RequestMapping ("/api/sample")
public class SampleController {

    private final SampleService sampleService;




}
