package main_project.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  =====================================================================
//  관리자 매칭 관리 목록의 검색 · 정렬 조건 (프론트 → 서버)
//
//  - 쓰이는 곳 : GET /api/admin/matchings?status=completed&keyword=로지스&transportType=true&sort=score&dir=desc&page=0
//      (MatchingController 의 @ModelAttribute — 주소 뒤 ?이름=값 들이 같은 이름의 필드에 자동으로 들어감)
//      조건이 많아서 @RequestParam 을 9개 나열하지 않고 DTO 하나로 받음 (무역 데이터 조건 검색의 SearchDto 와 같은 방식)
//  - 주소에 없는 조건은 null (= 그 조건으로 거르지 않음) , status · sort · dir · page 는 아래 기본값
//  - 화면에서는 각 조건이 해당 컬럼 바로 위(표 머리글 아래 필터 줄)에 있음
//  =====================================================================
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MatchingSearchDto {

//  진행 상태 : all / linked(자동 매칭 연결) / requested(물류 수락 대기) / completed(성사) / failed(실패)
    @Builder.Default
    private String status = "all";

//  화주사 → 운송사 칸 : 화주사 또는 운송사 회사 이름에 들어 있는 글자
    private String keyword;

//  추천 이유 · 주의 사항 칸 : 추천 이유 또는 주의 사항에 들어 있는 글자
    private String reason;

//  제공 조건 칸 (운송사 조건) : true / false , null 이면 전체
//  transportType : true = 해상 , false = 항공 (Lscore1Entity 의 transportType 과 같은 뜻)
    private Boolean transportType;

//  regularRoute : true = 정기 노선 O , false = 정기 노선 X
    private Boolean regularRoute;

//  directRoute : true = 직항 O , false = 직항 X
    private Boolean directRoute;

//  정렬 기준 : id(매칭 ID = 만들어진 순서) / score(매칭 점수)
    @Builder.Default
    private String sort = "id";

//  정렬 방향 : asc(오름차순) / desc(내림차순 — 기본 , 매칭 ID 내림차순 = 최신순)
    @Builder.Default
    private String dir = "desc";

//  몇 번째 페이지 (0 부터 — 0 = 1페이지)
    @Builder.Default
    private Integer page = 0;
}
