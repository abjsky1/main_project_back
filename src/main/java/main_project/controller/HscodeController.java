package main_project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.HscodeService;

@RestController
@RequestMapping("/api/hscode")
@RequiredArgsConstructor
public class HscodeController {

    private final HscodeService hscodeService;

    // Excel -> CSV 생성
    @GetMapping("/csv")
    public boolean createCsv() {

        return hscodeService.excelToCsv();
    }
}