package main_project.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.Cscore1Dto;
import main_project.model.dto.Cscore2Dto;
import main_project.model.dto.Cscore3Dto;
import main_project.model.dto.CscoreRequestDto;
import main_project.service.Cscore1Service;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController 
@RequestMapping ("/api/cscore")
@RequiredArgsConstructor 
public class Cscore1Controller {

    private final Cscore1Service cscore1Service;

    // [1] 화주 매칭 조건 등록
    @PostMapping("")
    public boolean cscoreWrite(@RequestBody CscoreRequestDto requestDto) {

        return cscore1Service.cscoreWrite(
            
                requestDto.getCscore1Dto(),

                requestDto.getCscore2Dto(),

                requestDto.getCscore3Dto());

    }

    // [2] Cscore1 전체조회
    @GetMapping("")
    public List<Cscore1Dto> cscoreRead(){

        return cscore1Service.cscoreRead();

    }

    // [3] Cscore1 개별 조회
    @GetMapping("/{cscore1Id}")
    public Cscore1Dto cscoreFindById(@PathVariable("cscore1Id") Integer cscore1Id) {

        return cscore1Service.cscoreFindByID(cscore1Id);

    }

    // [4] Cscore2 조회
    @GetMapping("/{cscore1Id}/cscore2")
    public Cscore2Dto cscore2Read(@PathVariable("cscore1Id") Integer cscore1Id) {

        return cscore1Service.cscore2Read(cscore1Id);

    }


    // [5] Cscore3 조회
    @GetMapping("/{cscore1Id}/cscore3")
    public Cscore3Dto cscore3Read(@PathVariable("cscore1Id") Integer cscore1Id) {

        return cscore1Service.cscore3Read(cscore1Id);

    }


    // [6] 화주 매칭 조건 삭제
    @DeleteMapping ("/{cscore1Id}")
    public boolean cscoreDelete(@PathVariable("cscore1Id") Integer cscore1Id) {

        return cscore1Service.cscoreDelete(cscore1Id);

    }

}



