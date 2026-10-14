package main_project.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationCountDto;
import main_project.model.dto.AuthorizationDto;
import main_project.model.entity.MemberEntity;
import main_project.model.entity.RoleEntity;
import main_project.model.repository.AuditRepository;
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

//  회원별 최근 로그인 시각 (감사 로그의 로그인 성공 기록)
    private final AuditRepository auditRepository;

    private final JwtUtil jwtUtil;

//  고객센터 채팅의 회원 캐시 지우기 (권한 · 상태가 바뀌면 채팅에도 바로 반영되게 — ChatService ⑪)
    private final ChatService chatService;


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
//
//  [회원 이름 · 이메일 암호화 이후 바뀐 점]
//  - 이름 · 이메일은 AES-GCM 으로 암호화되어(같은 글자도 매번 다른 암호문) DB 에서 LIKE 검색 · 정렬을 할 수 없음
//    → 역할 · 상태 조건만 DB 에서 거르고 , 회원을 엔티티로 읽어서(자동 복호화) 검색 · 정렬 · 50명씩 자르기는 자바에서 함
//  - 예전처럼 네이티브 쿼리로 이름 · 이메일을 바로 꺼내면 복호화가 되지 않아 화면에 암호문이 그대로 보임
//  ※ 회원 전체를 읽어서 처리하므로 회원이 수만 명이 되면 느려짐 (지금 약 200명이라 쿼리 2번 + 자바 처리로 충분)
    @Transactional (readOnly = true)
    public AuthorizationCountDto findAll(String name, String email, String roleName, Boolean status, String sort, String dir, int page){

    //  1. 값 정리
    //     - 페이지 번호가 0 보다 작으면 0 (주소를 직접 고쳐서 page=-1 같은 값이 와도 오류가 나지 않게)
    //     - 정렬 기준은 join / login , 방향은 asc / desc 만 인정
    //       "login".equals(sort) : sort 가 null 이어도 오류 없이 false
        if (page < 0) { page = 0; }
        if (!"login".equals(sort)) { sort = "join"; }
        if (!"asc".equals(dir)) { dir = "desc"; }

    //     - 검색어 : 빈 글자("" , "  ")는 검색 안 함(null) , 있으면 앞뒤 공백 제거 + 소문자 (대소문자 구분 없이 — 예전 MySQL LIKE 와 같게)
        if (name == null || name.isBlank()) { name = null; } else { name = name.trim().toLowerCase(Locale.ROOT); }
        if (email == null || email.isBlank()) { email = null; } else { email = email.trim().toLowerCase(Locale.ROOT); }

    //  2. 역할 · 상태 조건에 맞는 회원 전체 조회 (역할까지 JOIN FETCH 로 같이)     → 쿼리 1번
    //     SELECT m.* , r.* FROM member m JOIN role r ... WHERE member_id NOT IN ('GUEST' , 'WITHDRAWN') AND ...
        List<MemberEntity> memberEntities = memberRepository.searchWithRole(roleName, status);

    //  3. 회원별 최근 로그인 시각 (회원 번호 → 시각)     → 쿼리 1번
    //     Collectors.toMap( 키 , 값 ) : 목록을 Map 으로 바꿈 → 회원마다 get( 회원 번호 ) 로 바로 꺼냄
        Map<String, LocalDateTime> lastLogins = auditRepository.findLastLogins().stream()
            .collect(Collectors.toMap( (lastLogin) -> lastLogin.getMemberId(), (lastLogin) -> lastLogin.getLastLoginAt() ));

    //  4. 이름 · 이메일 검색 (복호화된 평문에서 글자가 들어 있는지)
    //     final 변수로 옮기는 이유 : 람다( ) -> { } 안에서는 값이 바뀌지 않는 변수만 쓸 수 있음 (name · email 은 1번에서 바뀜)
        String nameKeyword = name;
        String emailKeyword = email;
        List<MemberEntity> filtered = memberEntities.stream().filter( (memberEntity) -> {
            String managerName = memberEntity.getManagerName() == null ? "" : memberEntity.getManagerName().toLowerCase(Locale.ROOT);
            String userEmail = memberEntity.getUserEmail() == null ? "" : memberEntity.getUserEmail().toLowerCase(Locale.ROOT);
            if (nameKeyword != null && !managerName.contains(nameKeyword)) { return false; }
            if (emailKeyword != null && !userEmail.contains(emailKeyword)) { return false; }
            return true;
        } ).toList();

    //  5. 정렬 기준 만들기 (예전 네이티브 쿼리의 ORDER BY 와 같은 순서)
    //     - Comparator : "두 회원 중 누가 앞인지" 정하는 규칙 , thenComparing : 앞 기준이 같을 때 다음 기준
    //     - 가입순       : 가입 시각 → 같으면 회원 아이디 (순서가 매번 같아야 페이지를 넘길 때 같은 회원이 두 번 나오거나 빠지지 않음)
    //     - 최근 로그인순 : 로그인 시각 → 같으면 가입순
    //       nullsFirst : 로그인 기록이 없는 회원(null)을 가장 작은 값으로 (MySQL 과 같게 → 오름차순이면 맨 앞 , 내림차순이면 맨 뒤)
    //     - 내림차순이면 reversed( ) 로 전체 순서를 뒤집음 (기본 = 가입순 내림차순 = 최근 가입한 회원부터)
        Comparator<MemberEntity> joinOrder = Comparator
            .comparing( (MemberEntity memberEntity) -> memberEntity.getCreatedAt(), Comparator.nullsFirst(Comparator.naturalOrder()) )
            .thenComparing( (memberEntity) -> memberEntity.getMemberId() );
        Comparator<MemberEntity> order = joinOrder;
        if ("login".equals(sort)) {
            order = Comparator
                .comparing( (MemberEntity memberEntity) -> lastLogins.get(memberEntity.getMemberId()), Comparator.nullsFirst(Comparator.naturalOrder()) )
                .thenComparing(joinOrder);
        }
        if (!"asc".equals(dir)) { order = order.reversed(); }

    //  6. 정렬 → 이번 페이지(50명)만 잘라서 DTO 로
    //     skip( 앞 페이지 인원 ) → limit( 50 ) : 예) 3페이지(번호 2) → 앞의 100명을 건너뛰고 50명
        List<AuthorizationDto> authorizationDtos = filtered.stream()
            .sorted(order)
            .skip((long) page * PAGE_SIZE)
            .limit(PAGE_SIZE)
            .map( (memberEntity) -> AuthorizationDto.from(memberEntity, lastLogins.get(memberEntity.getMemberId())) )
            .toList();

    //  7. 페이지 숫자 : 전체 인원 → 전체 페이지 수 (50명씩 , 나머지가 있으면 한 페이지 더 — 0명이면 0페이지)
        long totalCount = filtered.size();
        int totalPages = (int) ((totalCount + PAGE_SIZE - 1) / PAGE_SIZE);

    //  8. 사용자 수 세기 (요약 카드는 필터와 상관없이 전체 기준)     → COUNT 쿼리 2번
        int allUser = (int) memberRepository.countByMemberIdNotIn(SYSTEM_MEMBER_IDS);

        int activate = (int) memberRepository.countByStatusAndMemberIdNotIn(true, SYSTEM_MEMBER_IDS);

        int deactivate = allUser - activate;

    //  9. 사용자 수 + 이번 페이지 목록 + 페이지 숫자 → 응답 DTO 하나로 묶기 (응답 모양은 예전과 같음 — 화면은 고칠 것 없음)
        AuthorizationCountDto result = AuthorizationCountDto.builder()
            .allUser(allUser)
            .activate(activate)
            .deactivate(deactivate)
            .members(authorizationDtos)
            .page(page)
            .size(PAGE_SIZE)
            .totalPages(totalPages)
            .totalCount(totalCount)
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

    //  채팅 캐시에 남은 "관리자인지" 지우기 (변경이 확정된 뒤에 지움)
        chatService.forgetMember(memberId);

        return true;
    }


//  상태 변경 스위치 : 활성(true) ↔ 비활성(false)
//  비활성이 되면 LoginService 의 status 검사로 로그인이 막힘
    public boolean toggleStatus(String memberId){

        MemberEntity memberEntity = findRealMember(memberId);
        if (memberEntity == null) { return false; }

        memberEntity.setStatus(!memberEntity.getStatus());

    //  채팅 캐시에 남은 "활성 회원인지" 지우기 (변경이 확정된 뒤에 지움)
        chatService.forgetMember(memberId);

        return true;
    }


//  실제 회원만 찾기 (없거나 공통 계정 GUEST · WITHDRAWN 이면 null)
    private MemberEntity findRealMember(String memberId){

        if (memberId == null || SYSTEM_MEMBER_IDS.contains(memberId)) { return null; }

        return memberRepository.findById(memberId).orElse(null);
    }


}
