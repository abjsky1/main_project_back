package main_project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.TradestatusDto;
import main_project.service.TradestatusService;

@RestController
@RequestMapping("/api/cumulative")
@RequiredArgsConstructor
public class TradestatusController {

    private final TradestatusService tradestatusService;

    @GetMapping("/trade")
    public TradestatusDto getyear(@RequestParam("year") Integer year) {

        return tradestatusService.getYear(year);
    }
}