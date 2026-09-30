package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.HscodeDto;
import main_project.service.HscodeService;

@RestController
@RequestMapping("/api/hscode")
@RequiredArgsConstructor
public class HscodeController {

    private final HscodeService hscodeService;


    // 전체 조회
    @GetMapping
    public List<HscodeDto> findAll() {
        return hscodeService.findAll();
    }


    // HS CODE 단건 조회
    @GetMapping("/{hscodeId}")
    public HscodeDto findById(
        @PathVariable("hscodeId") String hscodeId
    ) {return hscodeService.findById(hscodeId);}
}