package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.Lscore1Dto;
import main_project.model.dto.Lscore2Dto;
import main_project.model.dto.Lscore3Dto;
import main_project.model.dto.LscoreRequestDto;
import main_project.service.Lscore1Service;

@RestController
@RequestMapping("/api/lscore")
@RequiredArgsConstructor
public class Lscore1Controller {

    private final Lscore1Service lscore1Service;


    // [1] 물류업체 매칭 조건 등록
    @PostMapping("")
    public boolean lscoreWrite(@RequestBody LscoreRequestDto requestDto) {

        return lscore1Service.lscoreWrite(
                requestDto.getLscore1Dto(),
                requestDto.getLscore2Dto(),
                requestDto.getLscore3Dto());

    }

    // [2] 회원 조회 (memberId 생략 시 전체 조회)
    @GetMapping("")
    public List<Lscore1Dto> lscoreRead(
            @RequestParam(name = "memberId", required = false) String memberId) {

        return lscore1Service.lscoreRead(memberId);

    }

    // [3] Lscore1 개별 조회
    @GetMapping("/{lscore1Id}")
    public Lscore1Dto lscoreFindById(@PathVariable("lscore1Id") Integer lscore1Id) {

        return lscore1Service.lscoreFindById(lscore1Id);

    }


    // [4] Lscore1에 연결된 Lscore2 조회
    @GetMapping("/{lscore1Id}/lscore2")
    public Lscore2Dto lscore2Read(@PathVariable("lscore1Id") Integer lscore1Id) {

        return lscore1Service.lscore2Read(lscore1Id);

    }


    // [5] Lscore1에 연결된 Lscore3 조회
    @GetMapping("/{lscore1Id}/lscore3")
    public Lscore3Dto lscore3Read(@PathVariable("lscore1Id") Integer lscore1Id) {

        return lscore1Service.lscore3Read(lscore1Id);

    }


    // [6] 물류업체 매칭 조건 삭제
    @DeleteMapping("/{lscore1Id}")
    public boolean lscoreDelete(@PathVariable("lscore1Id") Integer lscore1Id) {

        return lscore1Service.lscoreDelete(lscore1Id);

    }

}