package main_project.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main_project.audit.AuditTargets;
import main_project.model.dto.AuditDto;
import main_project.model.dto.ActionDto;
import main_project.model.dto.AuditPageDto;
import main_project.model.dto.AuditSaveDto;
import main_project.model.entity.ActionEntity;
import main_project.model.entity.AuditEntity;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.ActionRepository;
import main_project.model.repository.AuditRepository;
import main_project.model.repository.MemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

//  비회원(로그인 안 한 손님 , 미가입 이메일 로그인 시도 등) 로그를 연결할 공통 계정
//  → member 테이블에 'GUEST' 행이 있어야 함 (MainDBSampleData.sql 의 member INSERT 맨 끝 줄)
    public static final String GUEST_MEMBER_ID = "GUEST";

//  탈퇴한 회원의 로그를 옮겨 붙일 공통 계정 (회원은 삭제해도 로그는 남기기 위해)
//  → member 테이블에 'WITHDRAWN' 행이 있어야 함 (MainDBSampleData.sql 의 member INSERT 맨 끝 줄)
    public static final String WITHDRAWN_MEMBER_ID = "WITHDRAWN";

//  action_detail 컬럼 길이 (AuditEntity 와 같게)
    private static final int DETAIL_MAX_LENGTH = 300;

//  감사 로그 목록 한 페이지에 보여 줄 건수 (화면에서 바꿀 수 없게 서버에서 정함)
    private static final int PAGE_SIZE = 50;

    private final AuditRepository auditRepository;
    private final MemberRepository memberRepository;
    private final ActionRepository actionRepository;
    private final AuditTargets auditTargets;

//  action 테이블 캐시 : 작업 유형 이름 → action_id  (로그 저장할 때마다 action 테이블을 조회하지 않으려고)
    private Map<String, Integer> actionIdMap = null;


//  감사 로그 목록 조회 — 한 페이지(50건)씩 (필터 조건 + 최신순)
//  user   : 이메일/이름 검색어 , memberType : 회원 유형(선택칸) , actionId : 작업 유형 번호(선택칸)
//  ip     : IP 주소 검색어 , result : 성공(true)/실패(false) — 비어 있으면 조건 없음
//  period : 기간 (today / 7days / 30days / 6months / 1year , 그 밖의 값이나 null 이면 전체 기간)
//  page   : 몇 번째 페이지 (0 부터 — 0 = 1페이지)
//  돌려주는 것 : 이번 페이지 로그 + 페이지 버튼에 필요한 숫자 (AuditPageDto)
    @Transactional (readOnly = true)
    public AuditPageDto findAll(String user, String memberType, Integer actionId, String ip, Boolean result, String period, int page){

    //  1. 페이지 번호 정리 : 0 보다 작으면 0 (주소를 직접 고쳐서 page=-1 같은 값이 와도 오류가 나지 않게)
        if (page < 0) { page = 0; }

    //  2. 기간 → 시작 시각 (이 시각 이후에 생긴 로그만 조회 , null 이면 기간 조건 없음)
    //     - 오늘 : 오늘 0시 0분부터 (지금부터 24시간 전이 아니라 "날짜가 오늘인 것")
    //     - 나머지 : 지금 시각에서 거꾸로 (minusDays(7) = 7일 전 , minusMonths(6) = 6개월 전)
    //     - "글자".equals(period) : period 가 null 이어도 오류 없이 false (period.equals("today") 는 null 이면 오류)
        LocalDateTime from = null;
        if ("today".equals(period))   { from = LocalDate.now().atStartOfDay(); }
        if ("7days".equals(period))   { from = LocalDateTime.now().minusDays(7); }
        if ("30days".equals(period))  { from = LocalDateTime.now().minusDays(30); }
        if ("6months".equals(period)) { from = LocalDateTime.now().minusMonths(6); }
        if ("1year".equals(period))   { from = LocalDateTime.now().minusYears(1); }

    //  3. 페이지 요청 만들기 : "page 번째 페이지를 50건씩"
    //     PageRequest.of( 페이지 번호 , 한 페이지 건수 ) → Pageable (Repository 에 넘기면 LIMIT / OFFSET 이 붙음)
    //     정렬은 쿼리의 ORDER BY(최신순)를 그대로 씀
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

    //  4. 사용자 검색어 → 그 검색어가 이메일 또는 이름에 들어 있는 회원 번호 목록
    //     - 회원 이메일 · 이름은 암호화되어 DB 에서 LIKE 로 찾을 수 없음 (같은 글자도 매번 다른 암호문)
    //       → 회원을 엔티티로 읽으면 EncryptedStringConverter 가 자동으로 복호화 → 자바에서 글자 비교
    //     - 대소문자 구분 없이 (예전 MySQL LIKE 와 같게) : 양쪽을 소문자로 바꿔서 비교
    //     - 검색어가 없으면 사용자 조건 없음 (userFilter = false , userIds 는 쓰지 않는 자리 채우기 [""])
    //     - 맞는 회원이 없으면 [""] 그대로 → 회원 번호가 "" 인 회원은 없어서 결과 0건
    //     ※ 회원 전체를 읽어서 비교하므로 회원이 수만 명이 되면 느려짐 (지금 약 200명이라 문제없음)
    //       SELECT * FROM member  (검색어가 있을 때만)
        String userKeyword = blankToNull(user);
        boolean userFilter = userKeyword != null;
        List<String> userIds = List.of("");
        if (userFilter) {
            String keyword = userKeyword.toLowerCase(Locale.ROOT);
            List<String> foundIds = memberRepository.findAll().stream()
                .filter( (memberEntity) -> {
                    String email = memberEntity.getUserEmail() == null ? "" : memberEntity.getUserEmail().toLowerCase(Locale.ROOT);
                    String name = memberEntity.getManagerName() == null ? "" : memberEntity.getManagerName().toLowerCase(Locale.ROOT);
                    return email.contains(keyword) || name.contains(keyword);
                } )
                .map( (memberEntity) -> memberEntity.getMemberId() )
                .toList();
            if (!foundIds.isEmpty()) { userIds = foundIds; }
        }

    //  5. 조건에 맞는 감사 로그 중 이번 페이지만 조회 (사용자 , 회원 유형 , 작업 유형까지 같이)
    //     → 목록 쿼리 1번 + 전체 건수 COUNT 쿼리 1번 (Repository 의 countQuery)
        Page<AuditEntity> auditPage = auditRepository.search(userFilter, userIds, blankToNull(memberType), actionId, blankToNull(ip), result, from, pageable);

    //  6. 이번 페이지의 엔티티 목록 → DTO 목록
    //     getContent( ) : Page 상자 안의 "이번 페이지 목록" 만 꺼냄 (최대 50건)
        List<AuditDto> logs = auditPage.getContent().stream().map( (auditEntity) -> { return AuditDto.from(auditEntity); } ).toList();

    //  7. 목록 + 페이지 숫자 → 응답 DTO 하나로 묶기 (AuthorizationService 의 사용자 목록 응답과 같은 방식)
    //     getNumber( )        : 지금 페이지 번호 (0 부터)
    //     getTotalPages( )    : 전체 페이지 수
    //     getTotalElements( ) : 조건에 맞는 전체 로그 수
        return AuditPageDto.builder()
            .logs(logs)
            .page(auditPage.getNumber())
            .size(PAGE_SIZE)
            .totalPages(auditPage.getTotalPages())
            .totalCount(auditPage.getTotalElements())
            .build();
    }


//  작업 유형 목록 (감사 로그 필터 줄의 "작업 유형" 선택칸) — action 테이블 전체를 번호순으로
//  Sort.by("actionId") : 작업 유형 번호(1 , 2 , 3 ...) 순서 , 엔티티 → DTO 로 바꿔서 돌려줌
    @Transactional (readOnly = true)
    public List<ActionDto> findActions(){
        return actionRepository.findAll(Sort.by("actionId")).stream().map((actionEntity) -> {
            return ActionDto.from(actionEntity);
        }).toList();
    }


//  빈 검색어("" , "  ")는 조건 없음(null)으로
    private String blankToNull(String value){
        return (value == null || value.isBlank()) ? null : value.trim();
    }


//  감사 로그 1건 저장 (AOP 인 audit/AuditAspect 가 요청마다 호출)
    public void record(AuditSaveDto auditSaveDto){

    //  1. 작업 유형 이름 → action_id  (action 테이블에 없는 이름이면 저장하지 않고 경고만)
        Integer actionId = findActionId(auditSaveDto.getActionType());
        if (actionId == null) {
            log.warn("[감사로그] action 테이블에 없는 작업 유형이라 기록하지 않음 : '{}'", auditSaveDto.getActionType());
            return;
        }

    //  2. 회원 찾기
    //     - 회원 번호가 없으면 → 비회원(GUEST)
    //     - 회원 번호는 있는데 DB 에 없으면 → 방금 탈퇴(삭제)한 회원(WITHDRAWN)
    //       (회원 탈퇴 요청은 쿠키 속 회원 번호로 기록되는데 , 기록하는 시점에는 이미 회원이 삭제되어 있음)
        String memberId = auditSaveDto.getMemberId();
        if (memberId == null) {
            memberId = GUEST_MEMBER_ID;
        } else if (!memberRepository.existsById(memberId)) {
            memberId = WITHDRAWN_MEMBER_ID;
        }

    //  3. 대상 문구가 컬럼 길이(300)를 넘으면 자르기
        String detail = auditSaveDto.getActionDetail();
        if (detail != null && detail.length() > DETAIL_MAX_LENGTH) {
            auditSaveDto.setActionDetail(detail.substring(0, DETAIL_MAX_LENGTH));
        }

    //  4. 저장 (getReferenceById : SELECT 없이 번호만 연결)
        MemberEntity memberEntity = memberRepository.getReferenceById(memberId);
        ActionEntity actionEntity = actionRepository.getReferenceById(actionId);

        auditRepository.save(auditSaveDto.toEntity(memberEntity, actionEntity));
    }


//  작업 유형 이름 → action_id  (처음 한 번만 action 테이블 전체 조회 , 못 찾으면 새로 추가됐을 수 있으니 한 번 더 조회)
    private synchronized Integer findActionId(String actionType){

        if (actionIdMap == null) { actionIdMap = loadActionIdMap(); }

        Integer actionId = actionIdMap.get(actionType);

        if (actionId == null) {
            actionIdMap = loadActionIdMap();
            actionId = actionIdMap.get(actionType);
        }
        return actionId;
    }

    private Map<String, Integer> loadActionIdMap(){
        Map<String, Integer> map = new HashMap<>();
        for (ActionEntity actionEntity : actionRepository.findAll()) {
            map.put(actionEntity.getActionType(), actionEntity.getActionId());
        }
        return map;
    }


//  서버 시작이 끝나면 (샘플 데이터 INSERT 까지 끝난 뒤) 등록표 검사
//  - 등록표(AuditTargets)의 작업 유형 이름이 action 테이블에 다 있는지
//  - 비회원(GUEST) 계정이 member 테이블에 있는지
//  → 문제가 있으면 콘솔에 경고 (서버는 정상 실행)
    @EventListener(ApplicationReadyEvent.class)
    public void checkAuditTargets(){

        synchronized (this) { actionIdMap = loadActionIdMap(); }

        for (String actionType : auditTargets.actionTypes()) {
            if (!actionIdMap.containsKey(actionType)) {
                log.warn("[감사로그] 등록표의 작업 유형 '{}' 이(가) action 테이블에 없음 → DB action_type 또는 AuditTargets 이름을 맞춰주세요", actionType);
            }
        }

        if (!memberRepository.existsById(GUEST_MEMBER_ID)) {
            log.warn("[감사로그] 비회원 계정('{}')이 member 테이블에 없음 → 비회원 로그가 저장되지 않습니다", GUEST_MEMBER_ID);
        }

        if (!memberRepository.existsById(WITHDRAWN_MEMBER_ID)) {
            log.warn("[감사로그] 탈퇴 회원 계정('{}')이 member 테이블에 없음 → 회원 탈퇴가 실패합니다", WITHDRAWN_MEMBER_ID);
        }
    }

}
