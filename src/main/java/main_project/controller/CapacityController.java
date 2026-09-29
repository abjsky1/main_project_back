package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.CapacityService;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class CapacityController {

    private final CapacityService capacityService;
}
