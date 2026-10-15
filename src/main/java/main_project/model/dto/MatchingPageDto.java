package main_project.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  =====================================================================
//  관리자 매칭 관리 화면 한 번에 (서버 → 프론트)
//
//  - 쓰이는 곳 : GET /api/admin/matchings?status=completed&page=0 (MatchingController)
//  - 화면 위쪽 상태 카드 5칸의 숫자 + 아래 표에 들어갈 "고른 상태의 매칭 한 페이지(50건)" 를 한 번에 보냄
//      예) { "allCount": 700, "linkedCount": 400, "requestedCount": 100, "completedCount": 100, "failedCount": 100,
//            "matchings": [ ...50건... ], "page": 0, "size": 50, "totalPages": 2, "totalCount": 100 }
//  - 두 DTO 를 합친 모양
//      AuthorizationCountDto (사용자 수 + 사용자 목록) — 숫자 + 목록
//      AuditPageDto          (로그 한 페이지 + 페이지 숫자) — 페이징
//
//  - 왜 한 번에 보내나 : 화면이 상태별 API 를 5번 부르면 요청이 5번 오가고 , 숫자를 세려고 목록을 통째로 받아야 함
//    → 한 번의 요청으로 "숫자 5개(DB 가 COUNT 로 셈) + 지금 볼 50건" 만 받음
//  =====================================================================
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MatchingPageDto {

//  ---------- 상태별 건수 (화면 위쪽 카드 5칸 , 고른 상태와 상관없이 항상 전체 기준) ----------
//  Long : DB 의 COUNT · SUM 결과가 long 이라 그대로 받음

//  전체 매칭 수
    private Long allCount;

//  자동 매칭 연결 : 추천만 생성되고 화주 · 운송사 둘 다 아직 응답 전 (화주 WAITING , 운송사 WAITING , 최종 PENDING)
    private Long linkedCount;

//  물류 수락 대기 : 화주는 요청했고 운송사는 아직 응답 전 (화주 ACCEPTED , 운송사 WAITING , 최종 PENDING)
    private Long requestedCount;

//  최종 매칭 성사 (최종 COMPLETED)
    private Long completedCount;

//  최종 매칭 실패 : 화주 또는 운송사가 거절 (최종 FAILED)
    private Long failedCount;

//  ---------- 고른 상태의 매칭 목록 한 페이지 ----------

//  이번 페이지의 매칭 (최신순 , 최대 50건)
    private List<MatchingDto> matchings;

//  지금 페이지 번호 — 0 부터 (0 = 1페이지 , 화면에서 +1 해서 보여 줌)
    private Integer page;

//  한 페이지 건수 (50)
    private Integer size;

//  고른 상태의 전체 페이지 수 (예: 100건 ÷ 50 = 2페이지 , 매칭이 없으면 0)
    private Integer totalPages;

//  고른 상태의 전체 매칭 수 (예: "성사" 를 고르면 completedCount 와 같은 값)
    private Long totalCount;
}
