package main_project.service;

import java.util.ArrayList;

import java.util.List;

import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import main_project.service.MatchingAiService;

import main_project.model.dto.MatchingDto;

import main_project.model.dto.MatchingPageDto;

import main_project.model.dto.MatchingSearchDto;

import main_project.model.entity.Cscore1Entity;

import main_project.model.entity.Cscore2Entity;

import main_project.model.entity.Cscore3Entity;

import main_project.model.entity.Lscore1Entity;

import main_project.model.entity.Lscore2Entity;

import main_project.model.entity.Lscore3Entity;

import main_project.model.entity.MatchingEntity;

import main_project.model.repository.Cscore1Repository;

import main_project.model.repository.Cscore2Repository;

import main_project.model.repository.Cscore3Repository;

import main_project.model.repository.Lscore1Repository;

import main_project.model.repository.Lscore2Repository;

import main_project.model.repository.Lscore3Repository;

import main_project.model.repository.MatchingRepository;

@Service

@RequiredArgsConstructor

@Transactional

public class MatchingService {

    private final MatchingAiService matchingAiService;

    private final MatchingRepository matchingRepository;

    private final Cscore1Repository cscore1Repository;

    private final Cscore2Repository cscore2Repository;

    private final Cscore3Repository cscore3Repository;

    private final Lscore1Repository lscore1Repository;

    private final Lscore2Repository lscore2Repository;

    private final Lscore3Repository lscore3Repository;

    // 관리자 매칭 관리 화면 한 페이지에 보여 줄 건수 ([5-1] matchingAdminRead — 감사 로그와 같은 50건)
    private static final int ADMIN_PAGE_SIZE = 50;

    // [1] 수출입기업(화주)과 물류기업 1차 매칭 조건 확인

    public boolean matchingCheck(Integer cscore1Id, Integer lscore1Id) {

        // 1. 수출입기업(화주) 기본 조건 조회

        Cscore1Entity cscore1Entity = cscore1Repository.findById(cscore1Id).orElse(null);

        if (cscore1Entity == null) {

            return false;

        }

        // 2. 수출입기업(화주)의 요청 물량 및 희망 날짜 조건 조회

        Cscore2Entity cscore2Entity = cscore2Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 3. 수출입기업(화주)의 특수화물 조건 조회

        Cscore3Entity cscore3Entity = cscore3Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 필요 조건 하나라도 없으면 탈락

        if (cscore2Entity == null || cscore3Entity == null) {

            return false;

        }

        // 4. 물류업체 기본 조건 조회

        Lscore1Entity lscore1Entity = lscore1Repository.findById(lscore1Id).orElse(null);

        if (lscore1Entity == null) {

            return false;

        }

        // 5. 물류업체 가용 물량 및 가능 날짜 조회

        Lscore2Entity lscore2Entity = lscore2Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        // 6. 물류업체 특수화물 조건 조회

        Lscore3Entity lscore3Entity = lscore3Repository.findByLscore1Entity(lscore1Entity).orElse(null);

        if (lscore2Entity == null || lscore3Entity == null) {

            return false;

        }

        // 7. 조회한 데이터를 1차 필터에 전달

        return firstFilter(

                cscore1Entity,

                cscore2Entity,

                cscore3Entity,

                lscore1Entity,

                lscore2Entity,

                lscore3Entity

        );

    }

    // [2] 1차 필터 (필수조건 확인)

    private boolean firstFilter(

            Cscore1Entity cscore1Entity,

            Cscore2Entity cscore2Entity,

            Cscore3Entity cscore3Entity,

            Lscore1Entity lscore1Entity,

            Lscore2Entity lscore2Entity,

            Lscore3Entity lscore3Entity) {

        // [추가] 화주 회원 활성 상태 확인

        if (cscore1Entity.getMemberEntity().getStatus() == null
                || cscore1Entity.getMemberEntity().getStatus() == false) {

            return false;

        }

        // [추가] 물류기업 회원 활성 상태 확인

        if (lscore1Entity.getMemberEntity().getStatus() == null
                || lscore1Entity.getMemberEntity().getStatus() == false) {

            return false;

        }

        // [추가] 1. 화주 매칭 동의 확인

        if (cscore1Entity.getMatchingAgree() == null

                || cscore1Entity.getMatchingAgree() == false) {

            return false;

        }

        // [추가] 2. 물류기업 매칭 동의 확인

        if (lscore1Entity.getMatchingAgree() == null

                || lscore1Entity.getMatchingAgree() == false) {

            return false;

        }

        // [추가] 3. 국가 일치 여부

        if (cscore1Entity.getCountryId() == null

                || !cscore1Entity.getCountryId().equals(lscore1Entity.getCountryId())) {

            return false;

        }

        // [추가] 4. 수출입 유형 일치 여부

        if (cscore1Entity.getTradeType() == null

                || !cscore1Entity.getTradeType().equals(lscore1Entity.getTradeType())) {

            return false;

        }

        // 5. 출발지 일치 여부

        if (!cscore1Entity.getDeparture().equals(lscore1Entity.getDeparture())) {

            return false;

        }

        // 6. 도착지 일치 여부

        if (!cscore1Entity.getArrival().equals(lscore1Entity.getArrival())) {

            return false;

        }

        // 7. 운송방식 일치 여부

        if (!cscore1Entity.getTransportType().equals(lscore1Entity.getTransportType())) {

            return false;

        }

        // 8. 물류업체 가용 물량이 화주 요청 물량 미만이면 탈락

        if (lscore2Entity.getAvailableCapacity() < cscore2Entity.getRequestWeight()) {

            return false;

        }

        // [추가] 9. 일반 컨테이너 취급 가능 여부

        if (cscore3Entity.getGeneralContainer()

                && !lscore3Entity.getGeneralContainer()) {

            return false;

        }

        // 10. 냉장/냉동 운송 가능 여부

        if (cscore3Entity.getRefrigerated()

                && !lscore3Entity.getRefrigerated()) {

            return false;

        }

        // 11. 위험물 운송 가능 여부

        if (cscore3Entity.getDangerous()

                && !lscore3Entity.getDangerous()) {

            return false;

        }

        // 12. 중량물 운송 가능 여부

        if (cscore3Entity.getHeavyCargo()

                && !lscore3Entity.getHeavyCargo()) {

            return false;

        }

        // 13. 특수화물 운송 가능 여부

        if (cscore3Entity.getSpecialCargo()

                && !lscore3Entity.getSpecialCargo()) {

            return false;

        }

        // 위 조건에 해당하지 않으면 1차 필터 통과

        return true;

    }

    // [3] 2차 필터 (매칭 점수 계산)

    // 아래 5개 점수 계산 기준은 기존 원본 유지

    // 1. 루트(노선) 점수 계산 : 30점

    private int routeScore(Lscore1Entity lscore1Entity) {

        // 정기노선 O + 직항 O : 30점

        if (lscore1Entity.getRegularRoute() && lscore1Entity.getDirectRoute()) {

            return 30;

        }

        // 정기노선 O + 직항 X : 25점

        if (lscore1Entity.getRegularRoute() && !lscore1Entity.getDirectRoute()) {

            return 25;

        }

        // 정기노선 X + 직항 O : 22점

        if (!lscore1Entity.getRegularRoute() && lscore1Entity.getDirectRoute()) {

            return 22;

        }

        // 정기노선 X + 직항 X : 18점

        return 18;

    }

    // 2. 가용 물량 점수 계산 (가용량 ÷ 요청량 × 100)

    private Integer capacityScore(

            Cscore2Entity cscore2Entity,

            Lscore2Entity lscore2Entity) {

        double capacityRatio = lscore2Entity.getAvailableCapacity() / cscore2Entity.getRequestWeight() * 100;

        // 150% 이상 : 25점

        if (capacityRatio >= 150) {

            return 25;

        }

        // 120% 이상 : 23점

        if (capacityRatio >= 120) {

            return 23;

        }

        // 105% 이상 : 20점

        if (capacityRatio >= 105) {

            return 20;

        }

        // 100% 이상 : 15점

        if (capacityRatio >= 100) {

            return 15;

        }

        // 100% 미만은 1차 필터에서 탈락

        return 0;

    }

    // 3. HS CODE 품목 적합도 점수 계산

    private Integer itemScore(

            Cscore1Entity cscore1Entity,

            Lscore1Entity lscore1Entity) {

        // 화주 HS CODE 숫자만 남기기

        String cHsCode =

                cscore1Entity.getHsCode().replaceAll("[^0-9]", "");

        // 물류업체 HS CODE 숫자만 남기기

        String lHsCode =

                lscore1Entity.getHsCode().replaceAll("[^0-9]", "");

        // 10자리 일치 : 20점

        if (cHsCode.length() >= 10 &&

                lHsCode.length() >= 10 &&

                cHsCode.substring(0, 10).equals(lHsCode.substring(0, 10))) {

            return 20;

        }

        // 앞 8자리 일치 : 17점

        if (cHsCode.length() >= 8 &&

                lHsCode.length() >= 8 &&

                cHsCode.substring(0, 8).equals(lHsCode.substring(0, 8))) {

            return 17;

        }

        // 앞 6자리 일치 : 14점

        if (cHsCode.length() >= 6 &&

                lHsCode.length() >= 6 &&

                cHsCode.substring(0, 6).equals(lHsCode.substring(0, 6))) {

            return 14;

        }

        // 앞 4자리 일치 : 12점

        if (cHsCode.length() >= 4 &&

                lHsCode.length() >= 4 &&

                cHsCode.substring(0, 4).equals(lHsCode.substring(0, 4))) {

            return 12;

        }

        // 앞 2자리 일치 : 8점

        if (cHsCode.length() >= 2 &&

                lHsCode.length() >= 2 &&

                cHsCode.substring(0, 2).equals(lHsCode.substring(0, 2))) {

            return 8;

        }

        // 일치하지 않음

        return 0;

    }

    // 4. 일정 적합도 점수 계산 : 15점

    private Integer scheduleScore(

            Cscore2Entity cscore2Entity,

            Lscore2Entity lscore2Entity) {

        // 날짜 일(day)로 변환

        Long desiredDate = cscore2Entity.getDesiredDate().toEpochDay();

        Long availableDate = lscore2Entity.getAvailableDate().toEpochDay();

        Long dateDifference;

        // 희망 날짜가 더 크면

        if (desiredDate > availableDate) {

            dateDifference = desiredDate - availableDate;

        } else {

            dateDifference = availableDate - desiredDate;

        }

        // 날짜가 같으면 : 15점

        if (dateDifference == 0) {

            return 15;

        }

        // 날짜 차이가 3일 이내 : 13점

        if (dateDifference <= 3) {

            return 13;

        }

        // 날짜 차이가 7일 이내 : 10점

        if (dateDifference <= 7) {

            return 10;

        }

        // 날짜 차이가 14일 이내 : 7점

        if (dateDifference <= 14) {

            return 7;

        }

        // 14일 초과지만 같은 연도 및 월 : 5점

        if (cscore2Entity.getDesiredDate().getYear() == lscore2Entity.getAvailableDate().getYear()

                && cscore2Entity.getDesiredDate().getMonth() == lscore2Entity.getAvailableDate().getMonth()) {

            return 5;

        }

        // 그 외 : 0점

        return 0;

    }

    // 5. 운송 경험에 따른 점수 계산 : 10점

    private Integer experienceScore(Lscore1Entity lscore1Entity) {

        // 물류업체의 운송 경험 횟수

        Integer experienceCount = lscore1Entity.getExperienceCount();

        // 800회 이상 : 10점

        if (experienceCount >= 800) {

            return 10;

        }

        // 600 ~ 799회 : 8점

        if (experienceCount >= 600) {

            return 8;

        }

        // 400 ~ 599회 : 6점

        if (experienceCount >= 400) {

            return 6;

        }

        // 200 ~ 399회 : 4점

        if (experienceCount >= 200) {

            return 4;

        }

        // 1 ~ 199회 : 2점

        if (experienceCount >= 1) {

            return 2;

        }

        return 0;

    }

    // [4] 최종 자동 매칭 실행

    @Transactional

    // 기존 화주 등록 시 호출
    public boolean matchingWrite(Integer cscore1Id) {

        // 기존처럼 모든 물류기업을 검사
        return matchingWrite(cscore1Id, null);

    }

    public boolean matchingWrite(
        Integer cscore1Id,
        Integer onlyLscore1Id)  {

        // [수정] 1. 화주 기본 조건 조회 및 잠금

        Cscore1Entity cscore1Entity = cscore1Repository.findForUpdate(cscore1Id).orElse(null);

        if (cscore1Entity == null) {

            return false;

        }

        // [추가] 비활성 화주 회원의 자동 매칭 실행 방지
        if (cscore1Entity.getMemberEntity().getStatus() == null
                || cscore1Entity.getMemberEntity().getStatus() == false) {
            return false;
        }

        // 2. 화주 매칭 동의 여부 확인

        if (cscore1Entity.getMatchingAgree() == null || cscore1Entity.getMatchingAgree() == false) {

            return false;

        }

        // 추가
        // 이미 매칭 요청 중이거나 완료된 화물인지 확인
        List<MatchingEntity> blockingMatchings = matchingRepository.findBlockingRequests(cscore1Id);

        // 진행 중이거나 완료된 매칭이 있다면 새 추천 생성하지 않음
        if (!blockingMatchings.isEmpty()) {

            return false;

        }

        // 3. 화주 물량/일정 조건 조회

        Cscore2Entity cscore2Entity = cscore2Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        // 4. 화주 특수화물 조건 조회

        Cscore3Entity cscore3Entity = cscore3Repository.findByCscore1Entity(cscore1Entity).orElse(null);

        if (cscore2Entity == null || cscore3Entity == null) {

            return false;

        }

        // 5. 점수 계산에 필요한 데이터 확인

        if (cscore1Entity.getHsCode() == null

                || cscore2Entity.getDesiredDate() == null

                || cscore2Entity.getRequestWeight() == null

                || !Double.isFinite(cscore2Entity.getRequestWeight())

                || cscore2Entity.getRequestWeight() <= 0) {

            return false;

        }

        
        // [수정] 6. 물류기업 조회 범위 결정
        List<Lscore1Entity> lscore1Entities;

        if (onlyLscore1Id == null) {

            // 화주 조건 등록 시:
            // 기존처럼 매칭에 동의한 모든 물류기업 검사
            lscore1Entities =
                    lscore1Repository.findByMatchingAgreeTrue();

        } else {

            // 물류기업 조건 등록 시:
            // 방금 등록한 물류기업 한 곳만 검사
            lscore1Entities = new ArrayList<>();

            Lscore1Entity target =
                    lscore1Repository.findById(onlyLscore1Id)
                            .orElse(null);

            if (target != null) {
                lscore1Entities.add(target);
            }
        }


        // 조건을 통과한 업체가 있는지 확인할 변수
        boolean matchingResult = false;

        // [수정] 7. 등록된 물류기업을 하나씩 반복 비교
        for (Lscore1Entity lscore1Entity : lscore1Entities) {

            // [추가] 비활성 물류기업은 추천 후보에서 제외
            if (lscore1Entity.getMemberEntity().getStatus() == null
                    || lscore1Entity.getMemberEntity().getStatus() == false) {
                continue;
            }

            // 현재 물류업체 가용 물량 및 가능 날짜 조회

            Lscore2Entity lscore2Entity = lscore2Repository.findByLscore1Entity(lscore1Entity).orElse(null);

            // 현재 물류업체 특수화물 조건 조회

            Lscore3Entity lscore3Entity = lscore3Repository.findByLscore1Entity(lscore1Entity).orElse(null);

            // 비교할 조건이 없으면 다음 물류기업으로 이동

            if (lscore2Entity == null || lscore3Entity == null) {

                continue;

            }

            // 점수 계산에 필요한 데이터 확인

            if (lscore1Entity.getHsCode() == null

                    || lscore2Entity.getAvailableDate() == null

                    || lscore2Entity.getAvailableCapacity() == null

                    || !Double.isFinite(lscore2Entity.getAvailableCapacity())

                    || lscore2Entity.getAvailableCapacity() <= 0) {

                continue;

            }

            // 8. 화주와 물류업체의 필수조건 검사

            boolean filterResult = firstFilter(

                    cscore1Entity, cscore2Entity, cscore3Entity,

                    lscore1Entity, lscore2Entity, lscore3Entity);

            // 1차 필터 탈락 시 다음 물류기업 검사

            if (filterResult == false) {

                continue;

            }

            // 조건을 통과한 업체가 존재

            matchingResult = true;

            // [추가] 9. 동일한 화주-물류기업 조건의 중복 매칭 확인

            boolean exists = matchingRepository.existsByCscore1EntityCscore1IdAndLscore1EntityLscore1Id(cscore1Id, lscore1Entity.getLscore1Id());

            // 이미 추천된 업체는 중복 저장하지 않음

            if (exists) {

                continue;

            }

            // 10. 1차 필터 통과 업체의 2차 점수 계산

            // 노선 점수

            Integer routeScore = routeScore(lscore1Entity);

            // 가용 물량 점수

            Integer capacityScore = capacityScore(cscore2Entity, lscore2Entity);

            // 품목 적합도 점수

            Integer itemScore = itemScore(cscore1Entity, lscore1Entity);

            // 일정 적합도 점수

            Integer scheduleScore = scheduleScore(cscore2Entity, lscore2Entity);

            // 운송 경험 점수

            Integer experienceScore = experienceScore(lscore1Entity);

            // 11. 5개 점수 합산

            Integer totalScore =

                    routeScore

                    + capacityScore

                    + itemScore

                    + scheduleScore

                    + experienceScore;

            // [수정] 12. 현재 물류기업의 매칭 결과 Entity 생성

            MatchingEntity matchingEntity = MatchingEntity.builder()

                    .cscore1Entity(cscore1Entity)

                    .lscore1Entity(lscore1Entity)

                    .routeScore(routeScore)

                    .capacityScore(capacityScore)

                    .itemScore(itemScore)

                    .scheduleScore(scheduleScore)

                    .experienceScore(experienceScore)

                    .totalScore(totalScore)

                    .build();

            // 13. 현재 물류기업의 추천 결과 저장
            MatchingEntity savedMatchingEntity = matchingRepository.save(matchingEntity);

            if (savedMatchingEntity.getMatchingId() == null) {

                return false;

            }

            // 다음 물류기업도 계속 검사

        }

        return matchingResult;

    }

    // [5] 관리자 매칭 관리 화면 — 상태별 건수 5개 + 고른 상태의 매칭 한 페이지(50건)를 한 번에
    //  - search : 검색 · 정렬 조건 묶음 (MatchingSearchDto — 화면에서 각 컬럼 위 필터 줄에 있는 조건들)
    //      status        : all(전체) / linked(자동 매칭 연결) / requested(물류 수락 대기) / completed(성사) / failed(실패)
    //      keyword       : 화주사 · 운송사 회사 이름 검색어 , reason : 추천 이유 · 주의 사항 검색어
    //      transportType : 해상(true) / 항공(false) , regularRoute · directRoute : O(true) / X(false) — null 이면 전체
    //      sort          : id(매칭 ID 순) / score(매칭 점수순) , dir : asc(오름차순) / desc(내림차순)
    //      page          : 몇 번째 페이지 (0 부터 — 0 = 1페이지)
    //  - DB 조회 : 건수 쿼리 1번 + 목록 쿼리 1번 (+ 운송 기간을 위한 lscore2 조회가 매칭 1건마다 1번 — matchingDtoFrom)
    //  (예전에 상태마다 따로 있던 목록 조회 4개를 이 메소드 하나로 합침 — 감사 로그 페이징과 같은 방식)
    public MatchingPageDto matchingAdminRead(MatchingSearchDto search) {

        // 1. 상태 정리 : 정해진 5가지가 아니면(null , 오타 등) 전체로
        //    List.of( ... ).contains( 값 ) : 목록 안에 그 값이 있는지 (null 이어도 오류 없이 false)
        String status = search.getStatus();
        if (!List.of("all", "linked", "requested", "completed", "failed").contains(status)) {
            status = "all";
        }

        // 2. 검색어 2개 정리 : 빈 글자("" , "  ")는 검색 안 함(null) , 있으면 앞뒤 공백 제거
        String keyword = search.getKeyword();
        if (keyword == null || keyword.isBlank()) {
            keyword = null;
        } else {
            keyword = keyword.trim();
        }

        String reason = search.getReason();
        if (reason == null || reason.isBlank()) {
            reason = null;
        } else {
            reason = reason.trim();
        }

        // 3. 페이지 번호 정리 : 없거나 0 보다 작으면 0
        int page = search.getPage() == null ? 0 : search.getPage();
        if (page < 0) {
            page = 0;
        }

        // 4. 상태별 건수 5개 (카드 숫자 — 필터가 있으면 필터에 맞는 매칭의 건수) — 쿼리 1번 (MatchingRepository [8])
        //    운송사 조건 3개(true / false / null)는 그대로 넘김 (null = 그 조건으로 거르지 않음)
        MatchingRepository.StatusCount statusCount = matchingRepository.findStatusCount(
                keyword, reason, search.getTransportType(), search.getRegularRoute(), search.getDirectRoute());

        // 5. 고른 상태의 전체 건수 → 전체 페이지 수 계산
        //    4번에서 이미 셌으니 목록 쪽에서 COUNT 쿼리를 다시 보내지 않음
        long totalCount = statusCount.getAllCount();
        if ("linked".equals(status))    { totalCount = statusCount.getLinkedCount(); }
        if ("requested".equals(status)) { totalCount = statusCount.getRequestedCount(); }
        if ("completed".equals(status)) { totalCount = statusCount.getCompletedCount(); }
        if ("failed".equals(status))    { totalCount = statusCount.getFailedCount(); }

        //    전체 페이지 수 = 건수 ÷ 50 을 올림 (예: 120건 → 2.4 → 3페이지 , 0건 → 0페이지)
        //    Math.ceil : 소수점 올림 , (double) : 나눗셈을 소수점까지 계산하려고 (정수 ÷ 정수는 소수점이 버려짐)
        int totalPages = (int) Math.ceil(totalCount / (double) ADMIN_PAGE_SIZE);

        // 6. 정렬 순서 만들기 (Sort = "어떤 칸으로 , 어느 방향으로" 정렬할지 적은 것)
        //    - 방향 : asc 면 오름차순(작은 것부터) , 그 밖의 값(null 포함)은 내림차순(큰 것부터 = 기본)
        //    - 매칭 점수순 : 점수가 같으면 매칭 ID 가 큰(최근) 매칭이 위로 → .and( ) 로 두 번째 기준을 이어 붙임
        //    - 매칭 ID 순(기본) : 매칭 ID(matchingId)는 만들어진 순서라서 내림차순 = 최신순
        //    Sort.by( 방향 , "엔티티 필드 이름" ) : 컬럼 이름(total_score)이 아니라 엔티티 필드 이름(totalScore)
        Sort.Direction direction = "asc".equals(search.getDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sortOrder = Sort.by(direction, "matchingId");
        if ("score".equals(search.getSort())) {
            sortOrder = Sort.by(direction, "totalScore").and(Sort.by(Sort.Direction.DESC, "matchingId"));
        }

        // 7. 고른 상태의 매칭 중 이번 페이지만 조회 — 쿼리 1번 (MatchingRepository [9])
        //    PageRequest.of( 페이지 번호 , 한 페이지 건수 , 정렬 ) : "page 번째 페이지를 50건씩 , 이 순서로"
        //    → 스프링이 쿼리 끝에 ORDER BY m.totalScore DESC, m.matchingId DESC LIMIT 50 OFFSET ... 을 붙여 줌
        Pageable pageable = PageRequest.of(page, ADMIN_PAGE_SIZE, sortOrder);
        List<MatchingEntity> matchingEntities = matchingRepository.findAdminPage(
                status, keyword, reason, search.getTransportType(), search.getRegularRoute(), search.getDirectRoute(), pageable);

        // 8. 엔티티 → DTO (회원별 조회 · 전체 조회와 같은 변환 [12] matchingDtoFrom 사용)
        List<MatchingDto> matchings = matchingEntities.stream().map((matchingEntity) -> {
            return matchingDtoFrom(matchingEntity);
        }).toList();

        // 9. 건수 5개 + 목록 + 페이지 숫자 → 응답 DTO 하나로
        return MatchingPageDto.builder()
                .allCount(statusCount.getAllCount())
                .linkedCount(statusCount.getLinkedCount())
                .requestedCount(statusCount.getRequestedCount())
                .completedCount(statusCount.getCompletedCount())
                .failedCount(statusCount.getFailedCount())
                .matchings(matchings)
                .page(page)
                .size(ADMIN_PAGE_SIZE)
                .totalPages(totalPages)
                .totalCount(totalCount)
                .build();

    }

    // [6] 회원별 매칭 조회

    public List<MatchingDto> matchingMemberRead(String memberId) {

        // [수정] 관리자 승인 여부와 관계없이 회원별 매칭 조회

        List<MatchingEntity> matchingEntities = matchingRepository.findVisibleToMember(memberId);

        List<MatchingDto> matchingDtos = new ArrayList<>();

        for (MatchingEntity matchingEntity : matchingEntities) {

            // [추가] 진행 중인 매칭은 양쪽 회원이 모두 활성인 경우에만 표시
            // 완료/실패 기록은 이전 이력이므로 그대로 표시
            if ("PENDING".equals(matchingEntity.getFinalStatus())) {

                if (matchingEntity.getCscore1Entity().getMemberEntity().getStatus() == null
                        || matchingEntity.getCscore1Entity().getMemberEntity().getStatus() == false) {
                    continue;
                }

                if (matchingEntity.getLscore1Entity().getMemberEntity().getStatus() == null
                        || matchingEntity.getLscore1Entity().getMemberEntity().getStatus() == false) {
                    continue;
                }
            }

            MatchingDto matchingDto = matchingDtoFrom(matchingEntity);

            matchingDtos.add(matchingDto);

        }

        return matchingDtos;

    }

    // [7] 수출입기업(화주) 매칭 수락
    // 추천된 물류기업에 매칭을 요청하는 기능
    public boolean shipperAccept(Integer matchingId, String memberId) {

        // 1. 매칭 조회 및 잠금
        MatchingEntity matchingEntity = matchingFindForChange(matchingId);

        if (matchingEntity == null) {

            return false;

        }

        // [추가] 화주와 물류기업 모두 활성 상태여야 요청/수락 가능
        if (matchingEntity.getCscore1Entity().getMemberEntity().getStatus() == null
                || matchingEntity.getCscore1Entity().getMemberEntity().getStatus() == false) {
            return false;
        }

        if (matchingEntity.getLscore1Entity().getMemberEntity().getStatus() == null
                || matchingEntity.getLscore1Entity().getMemberEntity().getStatus() == false) {
            return false;
        }

        // 2. 실제 화주 회원번호 확인
        String shipperMemberId = matchingEntity

                .getCscore1Entity()

                .getMemberEntity()

                .getMemberId();

        // 로그인 회원이 매칭 당사자가 아니면 처리 불가
        if (!shipperMemberId.equals(memberId)) {

            return false;

        }

        // 3. 이미 종료된 매칭이면 처리 불가
        if (!"PENDING".equals(matchingEntity.getFinalStatus())) {

            return false;

        }

        // 4. 화주가 아직 응답하지 않은 추천인지 확인
        if (!"WAITING".equals(matchingEntity.getShipperStatus())) {

            return false;

        }

        // 5. 물류기업도 아직 응답하지 않은 상태여야 함
        if (!"WAITING".equals(matchingEntity.getLogisticsStatus())) {

            return false;

        }

        // [추가] 6. 같은 화주 조건으로 이미 요청 또는 완료된 매칭 확인
        Integer cscore1Id = matchingEntity.getCscore1Entity().getCscore1Id();

        List<MatchingEntity> matchingEntities = matchingRepository.findBlockingRequests(cscore1Id);

        for (MatchingEntity existingMatchingEntity : matchingEntities) {

            if (!existingMatchingEntity.getMatchingId().equals(matchingId)) {

                // 이미 다른 물류기업과 진행 중이거나 완료
                return false;

            }

        }

        
        // 7. 선택한 운송사에 매칭 요청
        matchingEntity.setShipperStatus("ACCEPTED");

        // 물류기업 응답 전까지 PENDING 유지
        matchingRepository.save(matchingEntity);

        // 8. 같은 화물 조건의 모든 추천 조회
        // cscore1Id는 위에서 이미 선언되어 있음
        List<MatchingEntity> otherMatchings =
                matchingRepository.findByCscore1EntityCscore1Id(cscore1Id);

        // 9. 선택하지 않은 나머지 운송사 자동 거절
        for (MatchingEntity other : otherMatchings) {

            // 선택한 운송사 제외
            if (other.getMatchingId().equals(matchingId)) {
                continue;
            }

            // 아직 검토 대기 중인 추천만 자동 거절
            if ("PENDING".equals(other.getFinalStatus())
                    && "WAITING".equals(other.getShipperStatus())
                    && "WAITING".equals(other.getLogisticsStatus())) {

                other.setShipperStatus("AUTO_REJECTED");
                other.setFinalStatus("FAILED");

                matchingRepository.save(other);
            }
        }

        return true;

    }

    // [8] 수출입기업(화주) 매칭 거절
    public boolean shipperReject(Integer matchingId, String memberId) {

        MatchingEntity matchingEntity = matchingFindForChange(matchingId);

        if (matchingEntity == null) {

            return false;

        }

        // [추가] 요청한 회원 본인이 비활성이라면 거절 처리 불가
        // 상대방 비활성 상태는 기존 요청 정리를 위해 허용
        if (matchingEntity.getCscore1Entity().getMemberEntity().getStatus() == null
                || matchingEntity.getCscore1Entity().getMemberEntity().getStatus() == false) {
            return false;
        }

        // 실제 수출입기업 회원인지 확인
        String shipperMemberId = matchingEntity

                .getCscore1Entity()

                .getMemberEntity()

                .getMemberId();

        if (!shipperMemberId.equals(memberId)) {

            return false;

        }

        // 이미 종료된 매칭은 거절 불가
        if (!"PENDING".equals(matchingEntity.getFinalStatus())) {

            return false;

        }

        // 아직 요청하지 않은 추천만 거절 가능
        if (!"WAITING".equals(matchingEntity.getShipperStatus())) {

            return false;

        }

        if (!"WAITING".equals(matchingEntity.getLogisticsStatus())) {

            return false;

        }

        // 화주 거절 처리
        matchingEntity.setShipperStatus("REJECTED");

        // 매칭 실패 처리
        matchingEntity.setFinalStatus("FAILED");

        // DB 저장
        matchingRepository.save(matchingEntity);

        return true;

    }

    // [9] 물류기업 매칭 수락
    // [수정] 관리자 승인 없이 즉시 매칭 완료
    public boolean logisticsAccept(Integer matchingId, String memberId) {

        MatchingEntity matchingEntity = matchingFindForChange(matchingId);

        if (matchingEntity == null) {

            return false;

        }

        // [추가] 화주와 물류기업 모두 활성 상태여야 요청/수락 가능
        if (matchingEntity.getCscore1Entity().getMemberEntity().getStatus() == null
                || matchingEntity.getCscore1Entity().getMemberEntity().getStatus() == false) {
            return false;
        }

        if (matchingEntity.getLscore1Entity().getMemberEntity().getStatus() == null
                || matchingEntity.getLscore1Entity().getMemberEntity().getStatus() == false) {
            return false;
        }

        // 실제 물류기업 회원번호 확인
        String logisticsMemberId = matchingEntity

                .getLscore1Entity()

                .getMemberEntity()

                .getMemberId();

        if (!logisticsMemberId.equals(memberId)) {

            return false;

        }

        // 매칭이 진행 중인지 확인
        if (!"PENDING".equals(matchingEntity.getFinalStatus())) {

            return false;

        }

        // 화주가 먼저 매칭 요청했는지 확인
        if (!"ACCEPTED".equals(matchingEntity.getShipperStatus())) {

            return false;

        }

        // 물류기업이 아직 응답하지 않은 상태인지 확인
        if (!"WAITING".equals(matchingEntity.getLogisticsStatus())) {

            return false;

        }

        // 물류기업 수락
        matchingEntity.setLogisticsStatus("ACCEPTED");

        // [수정] 관리자 승인 없이 자동 매칭 완료
        matchingEntity.setFinalStatus("COMPLETED");
        
        matchingRepository.save(matchingEntity);

        return true;

    }

    // [10] 물류기업 매칭 거절
    public boolean logisticsReject(Integer matchingId, String memberId) {

        MatchingEntity matchingEntity = matchingFindForChange(matchingId);

        if (matchingEntity == null) {

            return false;

        }

        // [추가] 요청한 회원 본인이 비활성이라면 거절 처리 불가
        // 상대방 비활성 상태는 기존 요청 정리를 위해 허용
        if (matchingEntity.getLscore1Entity().getMemberEntity().getStatus() == null
                || matchingEntity.getLscore1Entity().getMemberEntity().getStatus() == false) {
            return false;
        }

        // 실제 물류기업 회원번호 확인
        String logisticsMemberId = matchingEntity

                .getLscore1Entity()

                .getMemberEntity()

                .getMemberId();

        if (!logisticsMemberId.equals(memberId)) {

            return false;

        }

        // 매칭 진행 중인지 확인
        if (!"PENDING".equals(matchingEntity.getFinalStatus())) {

            return false;

        }

        // 화주가 먼저 요청한 매칭이어야 함
        if (!"ACCEPTED".equals(matchingEntity.getShipperStatus())) {

            return false;

        }

        // 아직 물류기업이 응답하지 않은 상태인지 확인
        if (!"WAITING".equals(matchingEntity.getLogisticsStatus())) {

            return false;

        }

        
        // 1. 물류기업 거절 처리
        matchingEntity.setLogisticsStatus("REJECTED");
        matchingEntity.setFinalStatus("FAILED");

        matchingRepository.save(matchingEntity);

        // 2. 해당 화주의 매칭 조건 번호 조회
        Integer cscore1Id =
                matchingEntity.getCscore1Entity().getCscore1Id();

        // 3. 같은 화주 조건의 기존 추천 목록 조회
        List<MatchingEntity> otherMatchings =
                matchingRepository.findByCscore1EntityCscore1Id(cscore1Id);

        // 4. 자동 거절되었던 추천만 다시 복구
        for (MatchingEntity other : otherMatchings) {

            if ("AUTO_REJECTED".equals(other.getShipperStatus())
                    && "FAILED".equals(other.getFinalStatus())
                    && "WAITING".equals(other.getLogisticsStatus())) {

                // 물류기업이 활성 상태이고 매칭에 동의한 경우만 복구
                if (Boolean.TRUE.equals(
                        other.getLscore1Entity().getMemberEntity().getStatus())
                        && Boolean.TRUE.equals(
                        other.getLscore1Entity().getMatchingAgree())) {

                    // 다시 추천 대기 상태로 변경
                    other.setShipperStatus("WAITING");
                    other.setFinalStatus("PENDING");

                    matchingRepository.save(other);

                }

            }

        }

        // 5. 기존 화주 조건으로 자동 매칭 다시 실행
        // 이전에 추천되지 않은 새로운 물류기업도 검사
        matchingWrite(cscore1Id);

        return true;


    }

    // [11] 매칭 상태 변경 전 조회 및 잠금
    // [추가] 동일 화주가 여러 물류기업에 동시에 요청하는 상황 방지
    private MatchingEntity matchingFindForChange(Integer matchingId) {

        if (matchingId == null) {

            return null;

        }

        // 1. 변경하려는 매칭 조회
        MatchingEntity matchingEntity = matchingRepository.findById(matchingId).orElse(null);

        if (matchingEntity == null) {

            return null;

        }

        // 2. 매칭에 연결된 화주 조건 번호 확인
        Integer cscore1Id = matchingEntity.getCscore1Entity().getCscore1Id();

        // 3. 화주 조건 조회 및 DB 잠금
        Cscore1Entity cscore1Entity = cscore1Repository.findForUpdate(cscore1Id).orElse(null);

        if (cscore1Entity == null) {

            return null;

        }

        // 4. 매칭 결과 다시 조회 및 DB 잠금
        matchingEntity = matchingRepository.findByMatchingId(matchingId).orElse(null);

        return matchingEntity;

    }

    // [12] MatchingEntity -> MatchingDto 변환
    // 기존 matchingMemberRead() 등에서
    // 중복 작성했던 DTO 변환 코드를 하나로 정리
    private MatchingDto matchingDtoFrom(MatchingEntity matchingEntity) {

        // [추가] 매칭된 물류기업의 기본 운송 조건
        Lscore1Entity logisticsCondition = matchingEntity.getLscore1Entity();

        // [추가] 매칭된 물류기업의 운송 일정 정보
        Lscore2Entity logisticsSchedule = lscore2Repository
                    .findByLscore1Entity(logisticsCondition)
                    .orElse(null);

        MatchingDto matchingDto = MatchingDto.builder()

                // 매칭 PK
                .matchingId(matchingEntity.getMatchingId())

                // 화주 매칭 조건 PK
                .cscore1Id(
                        matchingEntity.getCscore1Entity().getCscore1Id()
                )

                // 물류기업 매칭 조건 PK
                .lscore1Id(
                        matchingEntity.getLscore1Entity().getLscore1Id()
                )

                // 수출입기업 정보
                .shipperCompanyName(
                        matchingEntity.getCscore1Entity()
                                .getMemberEntity().getCompanyName()
                )
                .shipperContactName(
                        matchingEntity.getCscore1Entity()
                                .getMemberEntity().getManagerName()
                )
                .shipperBizNumber(
                        matchingEntity.getCscore1Entity()
                                .getMemberEntity().getBusinessRegNo()
                )
                .shipperPhone(
                        matchingEntity.getCscore1Entity()
                                .getMemberEntity().getUserPhone()
                )
                .shipperAddress(
                        matchingEntity.getCscore1Entity()
                                .getMemberEntity().getCompanyAddress()
                )

                // 물류기업 정보
                .logisticsCompanyName(
                        matchingEntity.getLscore1Entity()
                                .getMemberEntity().getCompanyName()
                )
                .logisticsContactName(
                        matchingEntity.getLscore1Entity()
                                .getMemberEntity().getManagerName()
                )
                .logisticsBizNumber(
                        matchingEntity.getLscore1Entity()
                                .getMemberEntity().getBusinessRegNo()
                )
                .logisticsPhone(
                        matchingEntity.getLscore1Entity()
                                .getMemberEntity().getUserPhone()
                )
                .logisticsAddress(
                        matchingEntity.getLscore1Entity()
                                .getMemberEntity().getCompanyAddress()
                )

                // [추가] 운송 서비스 정보

                // 운송 방식 (true = 해상, false = 항공)
                .logisticsTransportType(
                        logisticsCondition.getTransportType()
                )

                // 정기노선 여부
                .logisticsRegularRoute(
                        logisticsCondition.getRegularRoute()
                )

                // 직항 여부
                .logisticsDirectRoute(
                        logisticsCondition.getDirectRoute()
                )

                // HS코드 취급 경험
                .logisticsExperienceCount(
                        logisticsCondition.getExperienceCount()
                )

                // 평균 운송 기간
                .logisticsAverageTransitDays(
                        logisticsSchedule != null
                                ? logisticsSchedule.getAverageTransitDays()
                                : null
                )

                // 매칭 점수
                .routeScore(matchingEntity.getRouteScore())
                .capacityScore(matchingEntity.getCapacityScore())
                .itemScore(matchingEntity.getItemScore())
                .scheduleScore(matchingEntity.getScheduleScore())
                .experienceScore(matchingEntity.getExperienceScore())
                .totalScore(matchingEntity.getTotalScore())

                // 매칭 상태
                .adminStatus(matchingEntity.getAdminStatus())
                .shipperStatus(matchingEntity.getShipperStatus())
                .logisticsStatus(matchingEntity.getLogisticsStatus())
                .finalStatus(matchingEntity.getFinalStatus())

                // 추천 이유 및 경고 메시지
                .recommendReason(matchingEntity.getRecommendReason())
                .warningMessage(matchingEntity.getWarningMessage())

                // 매칭 생성일
                .createdAt(matchingEntity.getCreatedAt())

                .build();

        return matchingDto;
    
    }


    // [추가] 기존 매칭 조건을 Gemini 평가용 JSON으로 변환
    public String matchingAiJson(Integer matchingId, String memberId) {

        // 1. 기존 매칭 결과 조회
        MatchingEntity matchingEntity =
                matchingRepository.findById(matchingId).orElse(null);

        if (matchingEntity == null || memberId == null) {
            return null;
        }

        // 2. 연결된 화주와 물류기업 조건
        Cscore1Entity cscore1 = matchingEntity.getCscore1Entity();
        Lscore1Entity lscore1 = matchingEntity.getLscore1Entity();

        // 3. 본인의 매칭인지 확인
        String shipperId = cscore1.getMemberEntity().getMemberId();
        String logisticsId = lscore1.getMemberEntity().getMemberId();

        boolean shipper = memberId.equals(shipperId);
        boolean logistics = memberId.equals(logisticsId)
                && "ACCEPTED".equals(matchingEntity.getShipperStatus());

        if (!shipper && !logistics) {
            return null;
        }

        // 4. 물량과 일정 조건 조회
        Cscore2Entity cscore2 =
                cscore2Repository.findByCscore1Entity(cscore1).orElse(null);

        Lscore2Entity lscore2 =
                lscore2Repository.findByLscore1Entity(lscore1).orElse(null);

        if (cscore2 == null || lscore2 == null) {
            return null;
        }

        // 5. 필수 데이터 확인
        if (cscore1.getHsCode() == null
                || lscore1.getHsCode() == null
                || cscore2.getRequestWeight() == null
                || cscore2.getDesiredDate() == null
                || lscore2.getAvailableCapacity() == null
                || lscore2.getAvailableDate() == null) {
            return null;
        }

        // 6. 기존 매칭 조건을 JSON으로 변환
        return matchingAiService.createMatchingJson(
                cscore1, cscore2, lscore1, lscore2);
    }



    // [13] 기존 Controller 임시 호환용
    // A방식에서는 관리자 승인을 사용하지 않음
    // MatchingController 수정 시 이 메서드는 삭제 가능
    public boolean matchingApprove(Integer matchingId) {

        return false;

    }

    // [14] 기존 Controller 임시 호환용
    // A방식에서는 관리자 반려를 사용하지 않음
    // MatchingController 수정 시 이 메서드는 삭제 가능
    public boolean matchingReject(Integer matchingId) {

        return false;

    }

}
