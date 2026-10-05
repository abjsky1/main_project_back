package main_project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.service.RedisTokenService;

@RestController
@RequestMapping("/api/redis-token")
@RequiredArgsConstructor
public class RedisTokenController {

    private final RedisTokenService redisTokenService;

    @GetMapping
    public String getRefreshToken(
            @RequestParam("memberId") String memberId
    ) {

        return redisTokenService.getRefreshToken(memberId);
    }
}