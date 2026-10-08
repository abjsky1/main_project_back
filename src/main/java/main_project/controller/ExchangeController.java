package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.ExchangeDto;
import main_project.service.ExchangeService;

@RestController
@RequestMapping("/api/exchange")      // 프론트 Vite 프록시가 /api 로 시작하는 주소만 백엔드로 전달
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    // 월별 평균 환율 (2000 ~ 2026 , 전체 통화) — GET /api/exchange/month
    @GetMapping("/month")
    public List<ExchangeDto> month() {
        return exchangeService.month();
    }
}
