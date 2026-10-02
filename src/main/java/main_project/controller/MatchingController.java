package main_project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.MatchingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController 
@RequestMapping ("/api/matching")
@RequiredArgsConstructor 
public class MatchingController {

    private final MatchingService matchingService;

    // 1차 매칭 조건 테스트용
    @GetMapping("/check/{cscore1Id}/{lscore1Id}")
    public boolean matchingCheck(
        @PathVariable ("cscore1Id") Integer cscore1Id ,
        @PathVariable ("lscore1Id") Integer lscore1Id ) {

            return matchingService.matchingCheck(cscore1Id , lscore1Id);

        }

    // 매칭 실행
    @PostMapping("/run/{cscore1Id}")
    public boolean matchingWrite(
            @PathVariable("cscore1Id") Integer cscore1Id) {

        return matchingService.matchingWrite(cscore1Id);

    }
    
    
}
