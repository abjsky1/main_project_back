package main_project.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationCountDto;
import main_project.model.dto.AuthorizationDto;
import main_project.model.entity.MemberEntity;
import main_project.model.entity.RoleEntity;
import main_project.model.repository.MemberRepository;
import main_project.model.repository.RoleRepository;
import main_project.util.JwtUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthorizationService {

//  role 테이블 번호는 RoleEntity 의 USER_ROLE_ID / ADMIN_ROLE_ID , 관리자인지는 MemberEntity.isAdmin( ) 으로 확인

//  실제 회원이 아닌 감사 로그용 공통 계정 (사용자 목록 · 사용자 수 · 권한 변경에서 제외)
    private static final List<String> SYSTEM_MEMBER_IDS = List.of(AuditService.GUEST_MEMBER_ID, AuditService.WITHDRAWN_MEMBER_ID);

//  사용자 목록 한 페이지에 보여 줄 인원 (감사 로그 · 매칭 관리와 같은 50)
    private static final int PAGE_SIZE = 50;

    private final MemberRepository memberRepository;

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
        if (!memberEntity.isAdmin()) { return null; }

    //  6. 관리자 확인 완료 → 관리자 회원 번호 반환 (컨트롤러가 "내 계정인지" 비교할 때 사용)
        return memberId;
    }


//  사용자 권한 관리 사용자 목록 조회 — 한 페이지씩 (필터 조건 + 정렬로 DB 에서 조회)
//  name     : 이름 검색어 , email : 이메일 검색어 , 비어 있으면 조건 없음
//  roleName : ROLE_ADMIN / ROLE_USER , status : true(활성) / false(비활성) , null 이면 조건 없음
//  sort     : join(가입순) / login(최근 로그인순) , 그 밖의 값이면 join
//  dir      : asc(오름차순) / desc(내림차순) , 그 밖의 값이면 desc (가입순 내림차순 = 최근 가입한 회원부터 , 기본)
//  page     : 0 부터 (0 = 1페이지)
//  흐름은 감사 로그 AuditService.findAll 과 같음 : 페이지 요청 만들기 → Page 조회 → DTO 로 바꾸기 → 숫자와 같이 묶기
    @Transactional (readOnly = true)
    public AuthorizationCountDto findAll(String name, String email, String roleName, Boolean status, String sort, String dir, int page){

    //  1. 값 정리
    //     - 페이지 번호가 0 보다 작으면 0 (주소를 직접 고쳐서 page=-1 같은 값이 와도 오류가 나지 않게)
    //     - 정렬 기준은 join / login , 방향은 asc / desc 만 인정 (쿼리의 CASE WHEN 이 이 글자들로 정렬을 고름)
    //       "login".equals(sort) : sort 가 null 이어도 오류 없이 false
        if (page < 0) { page = 0; }
        if (!"login".equals(sort)) { sort = "join"; }
        if (!"asc".equals(dir)) { dir = "desc"; }

    //     - 검색어 : 빈 글자("" , "  ")는 검색 안 함(null) , 있으면 앞뒤 공백 제거
        if (name == null || name.isBlank()) { name = null; } else { name = name.trim(); }
        if (email == null || email.isBlank()) { email = null; } else { email = email.trim(); }

    //  2. 조건에 맞는 회원 중 이번 페이지(50명)만 조회      → 목록 쿼리 1번 + 전체 인원 COUNT 쿼리 1번
    //     PageRequest.of( 페이지 번호 , 한 페이지 인원 ) → Pageable (Repository 에 넘기면 LIMIT / OFFSET 이 붙음)
    //     정렬은 쿼리 안의 ORDER BY CASE WHEN 이 sort · dir 로 정해서 , 여기서는 Sort 를 넘기지 않음
    //     MemberRow : 역할 이름 · 최근 로그인까지 쿼리가 한 번에 만들어 준 한 줄 (MemberRepository)
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<MemberRepository.MemberRow> memberPage = memberRepository.searchPage(name, email, roleName, status, sort, dir, pageable);

    //  3. 쿼리 결과 → 표에 들어갈 DTO 목록 (DB 가 정렬한 순서 그대로)
    //     getContent( ) : Page 상자 안의 "이번 페이지 회원 목록" 만 꺼냄
        List<AuthorizationDto> authorizationDtos = memberPage.getContent().stream().map((memberRow)->{
            return AuthorizationDto.from(memberRow);
        }).toList();

    //  4. 사용자 수 세기 (요약 카드는 필터와 상관없이 전체 기준)     → COUNT 쿼리 2번
        int allUser = (int) memberRepository.countByMemberIdNotIn(SYSTEM_MEMBER_IDS);

        int activate = (int) memberRepository.countByStatusAndMemberIdNotIn(true, SYSTEM_MEMBER_IDS);

        int deactivate = allUser - activate;

    //  5. 사용자 수 + 이번 페이지 목록 + 페이지 숫자 → 응답 DTO 하나로 묶기
    //     getNumber( )        : 지금 페이지 번호 (0 부터)
    //     getTotalPages( )    : 전체 페이지 수
    //     getTotalElements( ) : 필터에 맞는 전체 인원
        AuthorizationCountDto result = AuthorizationCountDto.builder()
            .allUser(allUser)
            .activate(activate)
            .deactivate(deactivate)
            .members(authorizationDtos)
            .page(memberPage.getNumber())
            .size(PAGE_SIZE)
            .totalPages(memberPage.getTotalPages())
            .totalCount(memberPage.getTotalElements())
            .build();

        return result;
    }


//  권한 변경 스위치 : 관리자(2) ↔ 일반 사용자(1)
//  없는 회원 , 비회원 공통 계정(GUEST)이면 false
    public boolean toggleRole(String memberId){

        MemberEntity memberEntity = findRealMember(memberId);
        if (memberEntity == null) { return false; }

    //  지금 관리자면 → 일반 사용자 번호 , 아니면 → 관리자 번호
        int nextRoleId = memberEntity.isAdmin() ? RoleEntity.USER_ROLE_ID : RoleEntity.ADMIN_ROLE_ID;

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
