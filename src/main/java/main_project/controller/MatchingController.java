package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MatchingDto;
import main_project.service.MatchingService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController 
@RequestMapping ("/api/matching")
@RequiredArgsConstructor 
public class MatchingController {

    private final MatchingService matchingService;

    // 매칭 실행
    @PostMapping("/run/{cscore1Id}")
    public boolean matchingWrite(
            @PathVariable("cscore1Id") Integer cscore1Id) {

        return matchingService.matchingWrite(cscore1Id);

    }

    // 매칭 결과 전체 조회
    @GetMapping("")
    public List<MatchingDto> matchingRead() {
        return matchingService.matchingRead();
    }
    
    
}
