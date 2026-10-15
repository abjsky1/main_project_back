package main_project.exchange.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.exchange.dto.ExchangeDto;
import main_project.exchange.service.ExchangeService;

@RestController
@RequestMapping("/api/exchange")      
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    // 월별 평균 환율 (2000 ~ 2026 , 전체 통화) — GET /api/exchange/monthly
    @GetMapping("/monthly")
    public List<ExchangeDto> month() {
        return exchangeService.month();
    }
}
