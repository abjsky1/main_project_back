package main_project.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

import main_project.model.dto.InsightDto.*;
import main_project.service.InsightService;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    // 개인화 응답 캐시 방지
    @ModelAttribute
    public void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    // [1] 회원 유형, 저장 조건, 국가, 기본 기간 조회
    // 예) GET /api/insights/options
    @GetMapping("/options")
    public Options insightOptions(
            @CookieValue(
                    name = "AccessToken",
                    required = false
            ) String accessToken
    ) {

        return insightService.options(accessToken);
    }

    // [2] 품목명 또는 HS 코드 검색
    // 예) GET /api/insights/items?q=330499
    @GetMapping("/items")
    public List<Item> insightItems(
            @RequestParam(name = "q") String query,
            @CookieValue(
                    name = "AccessToken",
                    required = false
            ) String accessToken
    ) {

        return insightService.items(
                accessToken,
                query
        );
    }

    // [3] 분석 생성 및 조건·결과 자동 저장
    // 예) POST /api/insights
    @PostMapping("")
    public Generation insightGenerate(
            @RequestBody Request request,
            @CookieValue(
                    name = "AccessToken",
                    required = false
            ) String accessToken
    ) {

        return insightService.generate(
                accessToken,
                request
        );
    }

    // [4] 본인의 분석 기록 목록
    // 예) GET /api/insights/history?page=0&size=10
    @GetMapping("/history")
    public HistoryPage insightHistoryList(
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            ) int page,
            @RequestParam(
                    name = "size",
                    defaultValue = "10"
            ) int size,
            @CookieValue(
                    name = "AccessToken",
                    required = false
            ) String accessToken
    ) {

        return insightService.historyList(
                accessToken,
                page,
                size
        );
    }

    // [5] 본인의 분석 기록 상세
    // 예) GET /api/insights/history/7
    @GetMapping("/history/{historyId}")
    public HistoryDetail insightHistoryDetail(
            @PathVariable("historyId") Integer historyId,
            @CookieValue(
                    name = "AccessToken",
                    required = false
            ) String accessToken
    ) {

        return insightService.historyDetail(
                accessToken,
                historyId
        );
    }

    // [6] 본인의 분석 기록 삭제
    // 예) DELETE /api/insights/history/7
    @DeleteMapping("/history/{historyId}")
    public boolean insightHistoryDelete(
            @PathVariable("historyId") Integer historyId,
            @CookieValue(
                    name = "AccessToken",
                    required = false
            ) String accessToken
    ) {

        return insightService.historyDelete(
                accessToken,
                historyId
        );
    }

    // 서비스 오류 응답
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseError(
            ResponseStatusException exception
    ) {

        return ResponseEntity
                .status(exception.getStatusCode())
                .header("Cache-Control", "no-store")
                .body(
                        Map.of(
                                "message",
                                Objects.toString(
                                        exception.getReason(),
                                        "요청을 처리하지 못했습니다."
                                )
                        )
                );
    }

    // JSON 형식 오류 응답
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonError(
            HttpMessageNotReadableException exception
    ) {

        return ResponseEntity
                .badRequest()
                .header("Cache-Control", "no-store")
                .body(
                        Map.of(
                                "message",
                                "요청 JSON의 자료형과 형식을 확인해 주세요."
                        )
                );
    }
}