package main_project.controller;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.ExchangeService;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
@RequiredArgsConstructor 
public class ExchangeController {

    private final ExchangeService exchangeService;
    
    @GetMapping("/month")
    public List<Map<String,Object>>month() {
        return exchangeService.month();
    }
    
}
