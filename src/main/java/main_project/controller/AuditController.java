package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.AuditService;

@RestController 
@RequestMapping ("")
@RequiredArgsConstructor 
public class AuditController {

    private final AuditService auditService;





}
