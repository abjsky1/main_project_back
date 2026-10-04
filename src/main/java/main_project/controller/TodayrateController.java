package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.TodayrateDto;
import main_project.service.TodayrateService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;

@RestController 
@RequestMapping ("/api/today")
@RequiredArgsConstructor 
public class TodayrateController {
    private final TodayrateService todayrateService;

    // 당일 환율 조회
    @GetMapping("/exchangerate")
    public List<TodayrateDto>getTodaylist() {
        return todayrateService.getTodaylist();
    } 
}
