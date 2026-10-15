package main_project.insight.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import main_project.insight.dto.InsightDto.*;
import main_project.insight.dto.InsightHistoryDto;
import main_project.insight.entity.InsightHistoryEntity;
import main_project.insight.repository.InsightHistoryRepository;
import main_project.member.entity.MemberEntity;
import main_project.member.repository.MemberRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InsightHistoryService {

    private final InsightHistoryRepository repository;
    private final ObjectMapper mapper;
    private final MemberRepository memberRepository;

    // [1] 분석 조건과 결과 저장
    @Transactional
    public Integer save(
            String memberId,
            ConditionSnapshot condition,
            Result result,
            JsonNode metadata
    ) {

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회원을 찾을 수 없습니다."
                ));

        String status;

        if ("not_requested".equals(result.getAiStatus())) {
            status = "NO_DATA";

        } else if ("ready".equals(result.getAiStatus())) {
            status = "COMPLETE";

        } else {
            status = "STATISTICS_ONLY";
        }

        try {
            Request filters = result.getFilters();

            InsightHistoryDto dto = InsightHistoryDto.builder()
                    .memberId(memberId)
                    .createdAt(result.getGeneratedAt())
                    .status(status)
                    .itemName(result.getItemName())
                    .hsCode(filters.getHsCode())
                    .direction(filters.getDirection())
                    .startMonth(filters.getStart())
                    .endMonth(filters.getEnd())
                    .conditionJson(
                            mapper.writeValueAsString(condition)
                    )
                    .resultJson(
                            mapper.writeValueAsString(result)
                    )
                    .metadataJson(
                            mapper.writeValueAsString(metadata)
                    )
                    .build();

            InsightHistoryEntity entity = repository.saveAndFlush(
                    dto.toEntity(member)
            );

            return entity.getInsightId();

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "분석 기록을 JSON으로 변환하지 못했습니다.",
                    exception
            );
        }
    }

    // [2] 본인 기록 목록 조회
    @Transactional(readOnly = true)
    public HistoryPage list(
            String memberId,
            int page,
            int size
    ) {

        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "페이지는 0 이상, 조회 개수는 1~50이어야 합니다."
            );
        }

        var result =
                repository.findByMember_MemberIdOrderByCreatedAtDescInsightIdDesc(
                        memberId,
                        PageRequest.of(page, size)
                );

        var items = result.getContent()
                .stream()
                .map(entity -> HistoryItem.builder()
                        .id(entity.getInsightId())
                        .createdAt(entity.getCreatedAt())
                        .status(entity.getStatus())
                        .itemName(entity.getItemName())
                        .hsCode(entity.getHsCode())
                        .direction(entity.getDirection())
                        .start(entity.getStartMonth())
                        .end(entity.getEndMonth())
                        .build()
                )
                .toList();

        return HistoryPage.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    // [3] 본인 기록 상세 조회
    @Transactional(readOnly = true)
    public HistoryDetail detail(
            String memberId,
            Integer historyId
    ) {

        InsightHistoryDto dto = InsightHistoryDto.from(
                owned(memberId, historyId)
        );

        try {
            return HistoryDetail.builder()
                    .id(dto.getInsightId())
                    .createdAt(dto.getCreatedAt())
                    .status(dto.getStatus())
                    .condition(
                            mapper.readValue(
                                    dto.getConditionJson(),
                                    ConditionSnapshot.class
                            )
                    )
                    .result(
                            mapper.readValue(
                                    dto.getResultJson(),
                                    Result.class
                            )
                    )
                    .metadata(
                            mapper.readTree(dto.getMetadataJson())
                    )
                    .build();

        } catch (JacksonException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "저장된 분석 기록을 읽지 못했습니다.",
                    exception
            );
        }
    }

    // [4] 본인 기록 삭제
    @Transactional
    public boolean delete(
            String memberId,
            Integer historyId
    ) {

        repository.delete(
                owned(memberId, historyId)
        );

        return true;
    }

    // 기록 번호와 로그인 회원을 함께 확인
    private InsightHistoryEntity owned(
            String memberId,
            Integer historyId
    ) {

        if (historyId == null || historyId < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "분석 기록 번호가 올바르지 않습니다."
            );
        }

        return repository.findByInsightIdAndMember_MemberId(
                        historyId,
                        memberId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "분석 기록을 찾을 수 없습니다."
                        )
                );
    }
}