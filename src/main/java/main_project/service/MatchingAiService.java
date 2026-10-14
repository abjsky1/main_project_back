package main_project.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import main_project.model.dto.MatchingAiDto;
import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore2Entity;
import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.Lscore2Entity;
import reactor.util.retry.Retry;


@Service 
public class MatchingAiService {

    // Gemini API 설정
    @Value ("${api.gemini-key}")
    private String geminiApiKey;

    // API 호출 및 JSON 변환
    private final WebClient webClient = WebClient.builder().build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // [1] TXT 파일 불러오기 (프롬프트 + 근거 문헌)
    public String prompt( String matchingJson ) {

        try {

            // 1. 프롬프트 파일 불러오기
            ClassPathResource promptFile = new ClassPathResource("Llm/MatchingPrompt.txt");

            // 2. 근거 문헌 TXT파일 불러오기
            ClassPathResource evidenceFile = new ClassPathResource("Llm/MatchingEvidence.txt");

            // 3. TXT 파일 내용 읽기
            String promptText;

            String evidenceText;

            try ( 

                InputStream promptInput = promptFile.getInputStream();

                InputStream evidenceInput = evidenceFile.getInputStream()

            ) {

                // TXT 내용을 String으로 읽기
                promptText = new String(promptInput.readAllBytes(),StandardCharsets.UTF_8);

                evidenceText = new String(evidenceInput.readAllBytes(),StandardCharsets.UTF_8);

            }

            // 4. 프롬프트 + 근거 문헌 + DB샘플데이터 조합
            String finalPrompt = promptText.replace("{{REFERENCE_TEXT}}", evidenceText)
                                           .replace("{{MATCHING_JSON}}", matchingJson);

            // 5. 최종 반환
            return finalPrompt;

        } catch (Exception e) {

            System.out.println("TXT 파일 오류 : " + e);

        }

        return null;

    }

    // [2] Gemini API 호출
    public String LLMCall(String prompt) {

        try{

            // 1. Gemini에 전달할 데이터
            Map<String,Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of(
                        "parts", List.of(
                                Map.of(
                                    "text", prompt)
                                    )
                    )

                ),
                // Json 형식으로 응답 요청
                "generationConfig", Map.of(
                "responseMimeType" , "application/json"
                )
            );

            // 2. Gemini API 요청
            String response = webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent")
                .header("x-goog-api-key", geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                 // 연결 오류가 발생하면 2초 후 한 번 재시도
                .retryWhen(Retry.backoff(1, Duration.ofSeconds(2))
                                .filter(e -> e instanceof WebClientRequestException))
                // 전체 응답 대기 최대 90초
                .block(Duration.ofSeconds(90));

            // 3. 응답 JSON 변혼
            JsonNode json = objectMapper.readTree(response);

            // 4. Gemini 생성한 텍스트 꺼내기
            String result = json.path("candidates")
                                .path(0)
                                .path("content")
                                .path("parts")
                                .path(0)
                                .path("text")
                                .asText();
            
                if (result.isEmpty()) {

                    throw new IllegalStateException("응답 내용이 없습니다.");
                    
                }

                return result;

            } catch (Exception e) {

                System.out.println("Gemini 평가 오류 : " + e.getMessage());
                e.printStackTrace();

                return null;
            }

    }

    // [3] AI 매칭 평가 실행
    public String chat(String matchingJson) {

        // 1. 최종 프롬프트
        String finalPrompt = prompt(matchingJson);

        // 생성 확인
        if (finalPrompt == null) {

            System.out.println("생성 실패");

            return null;
            
        }

        // 2. Gemini API 호출
        String result = LLMCall(finalPrompt);

        // 3. 결과 반환
        return result;

    }

    // [4] 실제 DB 매칭 데이터를 JSON 으로 변환하여 AI 평가
    // 변경
    public String createMatchingJson(
        Cscore1Entity cscore1Entity,
        Cscore2Entity cscore2Entity,
        Lscore1Entity lscore1Entity,
        Lscore2Entity lscore2Entity) {

        try {

            // 1. 화주 매칭 조건
            Map<String, Object> shipper = new HashMap<>();

            // HS코드
            shipper.put("hsCode",
                    cscore1Entity.getHsCode().replaceAll("[^0-9]", ""));

            // 요청 물량
            shipper.put("requestWeight",
                    cscore2Entity.getRequestWeight());

            // 희망 출발일
            shipper.put("desiredDate",
                    cscore2Entity.getDesiredDate().toString());

            // 물량 단위
            shipper.put("unit", "ton");


            // 2. 물류기업 매칭 조건
            Map<String, Object> logistics = new HashMap<>();

            // HS코드
            logistics.put("hsCode",
                    lscore1Entity.getHsCode().replaceAll("[^0-9]", ""));

            // 정기노선 여부
            logistics.put("regularRoute",
                    lscore1Entity.getRegularRoute());

            // 직항 여부
            logistics.put("directRoute",
                    lscore1Entity.getDirectRoute());

            // 가용 물량
            logistics.put("availableCapacity",
                    lscore2Entity.getAvailableCapacity());

            // 운송 가능 출발일
            logistics.put("availableDate",
                    lscore2Entity.getAvailableDate().toString());

            // 운송 경험
            logistics.put("experienceCount",
                    lscore1Entity.getExperienceCount());

            // 운송 방식
            logistics.put("transportType",
                    lscore1Entity.getTransportType());

            // 물량 단위
            logistics.put("unit", "ton");


            // 3. 화주 + 물류기업 데이터 합치기
            Map<String, Object> matchingData = Map.of(
                    "shipper", shipper,
                    "logistics", logistics
            );


            // 4. Map 데이터를 JSON 문자열로 변환
            String matchingJson =
                    objectMapper.writeValueAsString(matchingData);


            // 5. JSON 문자열 반환
            return matchingJson;


        } catch (Exception e) {

            System.out.println("매칭 데이터 처리 오류 : " + e);

            throw new IllegalStateException("매칭 실패", e);

        }

    }


    // [추가] JSON으로 Gemini AI 평가 실행
    public MatchingAiDto evaluateJson(String matchingJson) {

        try {

            // 1. Gemini API 호출
            String result = chat(matchingJson);

            // 2. 응답 확인
            if (result == null || result.isBlank()) {
                throw new IllegalStateException("평가 결과가 없습니다.");
            }

            // 3. JSON을 DTO로 변환
            MatchingAiDto matchingAiDto =
                    objectMapper.readValue(result, MatchingAiDto.class);

            // 여기 아래에 추가
            System.out.println("Gemini 평가 상태 : " + matchingAiDto.getEvaluationStatus());
            System.out.println("Gemini 평가 주의사항 : " + matchingAiDto.getWarningMessage());

            // 4. 점수 기본 검증
            validateScore(matchingAiDto);

            // 5. 평가 결과 반환
            return matchingAiDto;

        } catch (Exception e) {

            System.out.println("Gemini 평가 상세 오류 : " + e);
            e.printStackTrace();

            throw new IllegalStateException("매칭 평가 실패", e);
        }
    }


    // [5] Gemini AI 점수 기본 검증
    private void validateScore(MatchingAiDto aiDto) {

        // 1. 정상 평가 여부
        if (aiDto == null
                || !"SCORED".equals(aiDto.getEvaluationStatus())) {

            throw new IllegalStateException("평가가 완료되지 않았습니다.");
        }

        // 2. 점수 누락 확인
        if (aiDto.getRouteScore() == null
                || aiDto.getCapacityScore() == null
                || aiDto.getItemScore() == null
                || aiDto.getScheduleScore() == null
                || aiDto.getExperienceScore() == null
                || aiDto.getTotalScore() == null) {

            throw new IllegalStateException("평가 점수가 누락되었습니다.");
        }

        // 3. 프롬프트의 고정 점수표 확인
        if (!List.of(30, 25, 22, 18).contains(aiDto.getRouteScore())
                || !List.of(25, 23, 20, 15).contains(aiDto.getCapacityScore())
                || !List.of(20, 17, 14, 12, 8, 0).contains(aiDto.getItemScore())
                || !List.of(15, 13, 10, 7, 5, 0).contains(aiDto.getScheduleScore())
                || !List.of(10, 8, 6, 4, 2, 0).contains(aiDto.getExperienceScore())) {

            throw new IllegalStateException("점수 기준이 올바르지 않습니다.");
        }

        // 4. 항목별 점수 합산
        Integer totalScore = aiDto.getRouteScore()
                + aiDto.getCapacityScore()
                + aiDto.getItemScore()
                + aiDto.getScheduleScore()
                + aiDto.getExperienceScore();

        // 5. 총점 확인
        if (!totalScore.equals(aiDto.getTotalScore())) {

            throw new IllegalStateException("총점이 일치하지 않습니다.");
        }

        // 6. 추천 이유 확인
        if (aiDto.getRecommendReason() == null
                || aiDto.getRecommendReason().isBlank()
                || aiDto.getRecommendReason().length() > 300) {

            throw new IllegalStateException("추천 이유가 올바르지 않습니다.");
        }

        // 7. 주의사항 길이 확인
        if (aiDto.getWarningMessage() != null
                && aiDto.getWarningMessage().length() > 300) {

            throw new IllegalStateException("주의사항이 300자를 초과했습니다.");
        }

        // 8. 검증 완료
        System.out.println("점수 검증 완료 : " + totalScore + "점");

    }

    

}
