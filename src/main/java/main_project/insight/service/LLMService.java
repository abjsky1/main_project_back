package main_project.insight.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LLMService {

    @Value("${api.gemini-key:}")
    private String geminiApiKey;

    private static final String MODEL = "gemini-3.5-flash-lite";

    // 모델명이 한 번만 들어가도록 구성
    private static final String MODEL_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL
                    + ":generateContent";

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(60);

    private final ObjectMapper objectMapper;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer ->
                    configurer.defaultCodecs()
                            .maxInMemorySize(1024 * 1024)
            )
            .build();

    // 분석 기록에 저장할 모델명
    public String modelId() {
        return MODEL;
    }

    // [1] 일반 질문: 첨부 예제의 chat1()에 해당
    public String LLMCall(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "질문을 입력해 주세요."
            );
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        return callGemini(requestBody);
    }

    // [2] 개인화 분석: 조회한 자료와 분석 지침 전달
    public JsonNode analyze(
            String systemPrompt,
            Object context,
            JsonNode schema
    ) {

        // Java 객체를 JSON 문자열로 변환
        String jsonContext =
                objectMapper.writeValueAsString(context);

        Map<String, Object> requestBody =
                new LinkedHashMap<>();

        // 분석 역할과 지침
        requestBody.put(
                "systemInstruction",
                Map.of(
                        "parts", List.of(
                                Map.of("text", systemPrompt)
                        )
                )
        );

        // 실제 분석 자료
        requestBody.put(
                "contents",
                List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", jsonContext)
                                )
                        )
                )
        );

        // 화면과 검증 코드에서 사용할 JSON 형식 지정
        requestBody.put(
                "generationConfig",
                Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", schema,
                        "maxOutputTokens", 8192
                )
        );

        String answer = callGemini(requestBody);

        // AI가 생성한 JSON 문자열을 객체로 변환
        JsonNode result = objectMapper.readTree(answer);

        if (result == null || !result.isObject()) {
            throw new IllegalStateException(
                    "GEMINI_INVALID_JSON_OBJECT"
            );
        }

        return result;
    }

    // [3] 공통 API 호출: 첨부 예제의 callGemini()에 해당
    private String callGemini(
            Map<String, Object> requestBody
    ) {

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_NOT_CONFIGURED"
            );
        }

        try {
            String response = webClient.post()
                    .uri(MODEL_URL)
                    .header("x-goog-api-key", geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null || response.isBlank()) {
                throw new IllegalStateException(
                        "GEMINI_EMPTY_RESPONSE"
                );
            }

            JsonNode root = objectMapper.readTree(response);

            if (root == null || !root.isObject()) {
                throw new IllegalStateException(
                        "GEMINI_INVALID_RESPONSE"
                );
            }

            JsonNode candidate =
                    root.path("candidates").path(0);

            if (candidate.isMissingNode()) {
                throw new IllegalStateException(
                        "GEMINI_NO_CANDIDATE"
                );
            }

            String finishReason =
                    candidate.path("finishReason").asText();

            if (!"STOP".equals(finishReason)) {
                throw new IllegalStateException(
                        "GEMINI_INCOMPLETE: " + finishReason
                );
            }

            // 응답의 content → parts → text 추출
            StringBuilder answer = new StringBuilder();

            for (JsonNode part :
                    candidate.path("content").path("parts")) {

                boolean thought =
                        part.path("thought").asBoolean(false);

                if (!thought && part.path("text").isTextual()) {
                    answer.append(
                            part.path("text").asText()
                    );
                }
            }

            String text = answer.toString().trim();

            if (text.isBlank()) {
                throw new IllegalStateException(
                        "GEMINI_EMPTY_TEXT"
                );
            }

            return text;

        } catch (Exception exception) {
            // 원인을 보존해 서버에서 실패 원인을 확인할 수 있도록 처리
            throw new IllegalStateException(
                    "GEMINI_CALL_FAILED",
                    exception
            );
        }
    }
}