package main_project.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationCountDto;
import main_project.model.dto.AuthorizationDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.AuditRepository;
import main_project.model.repository.MemberRepository;
import main_project.model.repository.RoleRepository;
import main_project.util.JwtUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthorizationService {

//  role 테이블 번호 (MainDBSampleData.sql : 1 = ROLE_USER , 2 = ROLE_ADMIN)
    private static final int ROLE_USER_ID = 1;
    private static final int ROLE_ADMIN_ID = 2;

//  실제 회원이 아닌 감사 로그용 공통 계정 (사용자 목록 · 사용자 수 · 권한 변경에서 제외)
    private static final List<String> SYSTEM_MEMBER_IDS = List.of(AuditService.GUEST_MEMBER_ID, AuditService.WITHDRAWN_MEMBER_ID);

    private final MemberRepository memberRepository;

    private final AuditRepository auditRepository;

    private final RoleRepository roleRepository;

    private final JwtUtil jwtUtil;


//  [관리자 확인] 요청한 사람이 관리자인지 확인 (관리자 API 4개가 맨 앞에서 호출)
//  - 관리자면 그 관리자의 회원 번호를 , 관리자가 아니면 null 을 돌려줌
//  - null 이 되는 경우 : 쿠키 없음(비로그인) / 만료 · 가짜 토큰 / 없는 회원 / 비활성 회원 / 일반 사용자
//  - 흐름은 springweb day058 MemberController 의 [3] 내 정보 조회 와 같음
//      쿠키 받기 → 토큰에서 회원 번호 꺼내기 → DB 에서 회원 조회
//  - 토큰에는 회원 번호만 들어 있어서 , 관리자인지는 DB 의 role 로 확인
//    (권한이 바뀌면 다음 요청부터 바로 반영됨 — 관리자에서 일반 사용자로 바뀐 사람은 바로 막힘)
    @Transactional (readOnly = true)
    public String getLoginAdminId(String accessToken){

    //  1. 쿠키가 없으면 비로그인
        if (accessToken == null) { return null; }

    //  2. 토큰에서 회원 번호 꺼내기 (만료되었거나 가짜 토큰이면 null)
        String memberId = jwtUtil.getMemberIdFromToken(accessToken);
        if (memberId == null) { return null; }

    //  3. DB 에서 회원 조회 (없는 회원이면 null)
        MemberEntity memberEntity = memberRepository.findById(memberId).orElse(null);
        if (memberEntity == null) { return null; }

    //  4. 비활성 회원이면 null (관리자였어도 비활성이면 막음)
        if (memberEntity.getStatus() == false) { return null; }

    //  5. 관리자(role 2)가 아니면 null
        if (memberEntity.getRoleEntity().getRoleId() != ROLE_ADMIN_ID) { return null; }

    //  6. 관리자 확인 완료 → 관리자 회원 번호 반환 (컨트롤러가 "내 계정인지" 비교할 때 사용)
        return memberId;
    }


//  사용자 권한 관리 사용자 목록 조회 (필터 조건으로 DB 에서 조회)
//  roleName : ROLE_ADMIN / ROLE_USER , status : true(활성) / false(비활성) , null 이면 조건 없음
    @Transactional (readOnly = true)
    public AuthorizationCountDto findAll(String roleName, Boolean status){

    //  1. 조건에 맞는 회원 조회 (역할까지 같이)                    → 쿼리 1번
        List<MemberEntity> memberEntities = memberRepository.searchWithRole(roleName, status);

    //  2. 회원별 최근 로그인 시간 조회                          → 쿼리 1번
        Map<String, LocalDateTime> lastLoginMap = new HashMap<>();

        auditRepository.findLastLoginList().forEach((lastLogin)->{
            lastLoginMap.put(lastLogin.getMemberId(), lastLogin.getLastLoginAt());
        });

    //  3. 회원 + 최근 로그인 시간 → 표에 들어갈 DTO 목록
        List<AuthorizationDto> authorizationDtos = memberEntities.stream().map((memberEntity)->{

            LocalDateTime lastLoginAt = lastLoginMap.get(memberEntity.getMemberId());   // 로그인 기록이 없으면 null

            return AuthorizationDto.from(memberEntity, lastLoginAt);

        }).toList();

    //  4. 사용자 수 세기 (요약 카드는 필터와 상관없이 전체 기준)     → COUNT 쿼리 2번
        int allUser = (int) memberRepository.countByMemberIdNotIn(SYSTEM_MEMBER_IDS);

        int activate = (int) memberRepository.countByStatusAndMemberIdNotIn(true, SYSTEM_MEMBER_IDS);

        int deactivate = allUser - activate;

    //  5. 사용자 수 + 목록 → 응답 DTO 하나로 묶기
        AuthorizationCountDto result = AuthorizationCountDto.builder()
            .allUser(allUser)
            .activate(activate)
            .deactivate(deactivate)
            .members(authorizationDtos)
            .build();

        return result;
    }


//  권한 변경 스위치 : 관리자(2) ↔ 일반 사용자(1)
//  없는 회원 , 비회원 공통 계정(GUEST)이면 false
    public boolean toggleRole(String memberId){

        MemberEntity memberEntity = findRealMember(memberId);
        if (memberEntity == null) { return false; }

        int nextRoleId = memberEntity.getRoleEntity().getRoleId() == ROLE_ADMIN_ID ? ROLE_USER_ID : ROLE_ADMIN_ID;

    //  setter 로 바꾸면 트랜잭션이 끝날 때 JPA 가 UPDATE 해줌 (dirty checking)
        memberEntity.setRoleEntity(roleRepository.getReferenceById(nextRoleId));

        return true;
    }


//  상태 변경 스위치 : 활성(true) ↔ 비활성(false)
//  비활성이 되면 LoginService 의 status 검사로 로그인이 막힘
    public boolean toggleStatus(String memberId){

        MemberEntity memberEntity = findRealMember(memberId);
        if (memberEntity == null) { return false; }

        memberEntity.setStatus(!memberEntity.getStatus());

        return true;
    }


//  실제 회원만 찾기 (없거나 공통 계정 GUEST · WITHDRAWN 이면 null)
    private MemberEntity findRealMember(String memberId){

        if (memberId == null || SYSTEM_MEMBER_IDS.contains(memberId)) { return null; }

        return memberRepository.findById(memberId).orElse(null);
    }


}
