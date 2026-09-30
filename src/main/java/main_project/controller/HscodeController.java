package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.HscodeDto;
import main_project.model.entity.HscodeEntity;
import main_project.service.HscodeService;

@RestController
@RequestMapping("/api/hscode")
@RequiredArgsConstructor
public class HscodeController {

    private final HscodeService hscodeService;


    // 1. Excel -> CSV 생성
    @GetMapping("/csv")
    public boolean createCsv() {return hscodeService.excelToCsv();}


    // 2. DB에 HS CODE 추가
    @PostMapping
    public boolean create(@RequestBody HscodeDto dto) {return hscodeService.create(dto);}

    // 3. DB 전체 조회
    @GetMapping
    public List<HscodeEntity> findAll() {return hscodeService.findAll();}

    // 4. DB 개별 조회
    @GetMapping("/{hscodeId}")
    public HscodeEntity findById(@PathVariable("hscodeId")String hscodeId) {return hscodeService.findById(hscodeId);}

}