package main_project.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.ExchangerateDto;
import main_project.service.ExchangerateService;
import main_project.service.SseService;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangerateController {

    private final SseService sseService;
    private final ExchangerateService exchangerateService;

    @GetMapping("/latest")
    public List<ExchangerateDto> getExchangerate() {
        return exchangerateService.getExchangerate();
    }

    @GetMapping ( value = "/stream" , produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe( ){
        System.out.println("ExchangerateController.subscribe()");
        return sseService.subscribe();
    }
}