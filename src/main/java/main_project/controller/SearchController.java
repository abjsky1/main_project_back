package main_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.SearchDto;
import main_project.model.dto.TradeSearchDto;
import main_project.service.SearchService;


@RestController 
@RequestMapping ("/api/condition")
@RequiredArgsConstructor 
public class SearchController { 
    private final SearchService searchService;

    @GetMapping("/search")
    public List<TradeSearchDto> getsearch(@ModelAttribute SearchDto searchDto) {
        return searchService.getsearch(searchDto);
    }
}
