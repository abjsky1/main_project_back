package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.ExchangerateDto;
import main_project.service.ExchangerateService;

@RestController
@RequestMapping("/api/exchangerate")
@RequiredArgsConstructor
public class ExchangerateController {

    private final ExchangerateService exchangerateService;

    @GetMapping("/nation")
    public List<ExchangerateDto> getExchangerate() {
        return exchangerateService.getExchangerate();
    }
}