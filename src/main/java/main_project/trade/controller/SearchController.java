package main_project.trade.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import main_project.trade.dto.SearchDto;
import main_project.trade.dto.TradeSearchDto;
import main_project.trade.service.SearchHistoryService;
import main_project.trade.service.SearchService;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SearchHistoryService searchHistoryService;

    @GetMapping("/search")
    public List<TradeSearchDto> getsearch( @ModelAttribute SearchDto searchDto,
        // 쿠키가 없어도 검색 가능(비로그인시 토큰 null)
        @CookieValue(value = "AccessToken", required = false)
        String accessToken
    ) {
        // 기존 검색 실행
        List<TradeSearchDto> result = searchService.getsearch(searchDto);
        // 추가: 로그인한 활성 회원의 조건만 저장
        searchHistoryService.save(accessToken, searchDto);
        // 검색 결과 반환
        return result;
    }
}
