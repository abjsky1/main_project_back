package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.TradeYearDto;
import main_project.service.TradeCsvService;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeCsvService tradeCsvService;


    @GetMapping("/test")
    public String test() {
        return "연결 성공";
    }


    @GetMapping("/year")
    public List<TradeYearDto> getYearComparison() {
        return tradeCsvService.getYearComparison();
    }
}