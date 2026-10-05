package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MonthlyTradeDto;
import main_project.model.dto.TradestatusDto;
import main_project.service.TradestatusService;

@RestController
@RequestMapping("/api/cumulative")
@RequiredArgsConstructor
public class TradestatusController {

    private final TradestatusService tradestatusService;

    // 총 수출입.무역수지.증감율 조회
    @GetMapping("/trade")
    public TradestatusDto getyear(@RequestParam("year") Integer year) {
        return tradestatusService.getYear(year);
    }
    // 월별 수출입.무역수지 조회
    @GetMapping("/monthly")
    public List<MonthlyTradeDto> getMonthly(@RequestParam("year") Integer year) {
        return tradestatusService.getMonthly(year);
    }
}