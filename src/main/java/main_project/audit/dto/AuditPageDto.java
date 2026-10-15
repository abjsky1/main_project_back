package main_project.audit.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  =====================================================================
//  감사 로그 한 페이지 (서버 → 프론트) — 페이지 처리(페이징) 응답
//
//  - 쓰이는 곳 : GET /api/admin/audits?page=0 ... (AuditController)
//  - 로그 "전체" 가 아니라 "이번 페이지 50건" + 페이지 버튼을 그리는 데 필요한 숫자들을 같이 보냄
//      예) { "logs": [ ...50건... ], "page": 2, "size": 50, "totalPages": 28, "totalCount": 1352 }
//  - AuthorizationCountDto(사용자 수 + 사용자 목록) 와 같은 모양 : 목록 + 숫자 몇 개
//
//  - 스프링의 Page 객체를 그대로 보내지 않고 이 DTO 에 옮겨 담는 이유
//      1. Page 를 그대로 JSON 으로 바꾸면 칸이 아주 많고 복잡함 (pageable , sort , first , last , empty ...)
//      2. 지금 쓰는 스프링 버전은 Page 를 그대로 보내면 "모양이 바뀔 수 있다" 는 경고를 띄움
//      → 화면에 필요한 칸만 골라 담아서 보냄 (엔티티를 그대로 보내지 않고 DTO 로 바꾸는 것과 같은 이유)
//  =====================================================================
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AuditPageDto {

//  이번 페이지의 로그 (최신순 , 최대 50건)
    private List<AuditDto> logs;

//  지금 페이지 번호 — 0 부터 시작 (0 = 1페이지 , 1 = 2페이지 ...)
//  스프링 페이징은 0 부터 세서 그대로 보냄 → 화면에서 사람에게 보여 줄 때 +1
    private Integer page;

//  한 페이지에 몇 건씩 (50)
    private Integer size;

//  전체 페이지 수 (예: 1352건 ÷ 50 = 27.04 → 남는 2건도 한 페이지가 필요해서 28페이지)
//  조건에 맞는 로그가 하나도 없으면 0
    private Integer totalPages;

//  조건에 맞는 전체 로그 수 (이번 페이지 건수가 아니라 전체 — 화면의 "총 1,352건")
//  Long : 스프링 Page 의 getTotalElements( ) 가 long 을 돌려줘서 그대로 받음
    private Long totalCount;
}
