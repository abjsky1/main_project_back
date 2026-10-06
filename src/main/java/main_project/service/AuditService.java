package main_project.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main_project.audit.AuditTargets;
import main_project.model.dto.AuditDto;
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

//  action_detail 컬럼 길이 (AuditEntity 와 같게)
    private static final int DETAIL_MAX_LENGTH = 300;

    private final AuditRepository auditRepository;
    private final MemberRepository memberRepository;
    private final ActionRepository actionRepository;
    private final AuditTargets auditTargets;

//  action 테이블 캐시 : 작업 유형 이름 → action_id  (로그 저장할 때마다 action 테이블을 조회하지 않으려고)
    private Map<String, Integer> actionIdMap = null;


//  감사 로그 목록 조회 (필터 조건 + 최신순)
//  user : 이메일/이름 검색어 , action : 작업 유형 검색어 , result : 성공(true)/실패(false) , 비어 있으면 조건 없음
    @Transactional (readOnly = true)
    public List<AuditDto> findAll(String user, String action, Boolean result){

    //  1. 조건에 맞는 감사 로그 조회 (사용자 , 회원 유형 , 작업 유형까지 같이)     → 쿼리 1번
        List<AuditEntity> auditEntities = auditRepository.search(blankToNull(user), blankToNull(action), result);

    //  2. 엔티티 → DTO 로 변환
        return auditEntities.stream().map((auditEntity)->{

            return AuditDto.from(auditEntity);

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

    //  2. 회원 찾기 : 회원 번호가 없거나 DB 에 없는 회원이면 비회원(GUEST)
        String memberId = auditSaveDto.getMemberId();
        if (memberId == null || !memberRepository.existsById(memberId)) {
            memberId = GUEST_MEMBER_ID;
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
    }

}
