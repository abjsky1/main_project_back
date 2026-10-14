package main_project.model.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import main_project.model.entity.MemberEntity;


@Repository 
public interface MemberRepository extends JpaRepository <MemberEntity , String> {


// 로그인용 회원 조회
Optional<MemberEntity> findByUserEmail(String userEmail);

//  사용자 권한 관리 목록 조회 — 한 페이지씩 (필터 조건 + 최근 로그인 정렬)
//  - roleName  : ROLE_ADMIN / ROLE_USER , status : true(활성) / false(비활성) → null 이면 그 조건은 빼고 조회
//  - loginSort : 'asc'(로그인한 지 오래된 사람부터) / 'desc'(최근 로그인한 사람부터) / null(정렬 없음 → 최근 가입순)
//  - 감사 로그용 공통 계정(비회원 GUEST , 탈퇴 회원 WITHDRAWN)은 실제 회원이 아니라서 제외
//
//  [왜 nativeQuery 인가]
//  - "최근 로그인 시간" 은 member 테이블에 없고 audit 테이블(로그인 성공 기록)에 있음
//  - 페이지를 나누기 전에 DB 에서 먼저 정렬해야 맞는 순서가 됨
//      (50명만 가져와서 화면에서 정렬하면 "이번 페이지 50명 안에서만" 정렬되어 전체 순서가 틀림)
//  - 그래서 회원마다 마지막 로그인 시간을 붙여서(LEFT JOIN) 그 시간으로 ORDER BY → MySQL 쿼리를 그대로 씀
//    → 엔티티 이름 대신 테이블 이름(member , role , audit) , 필드 이름 대신 컬럼 이름(role_id , created_at ...)
//
//  [쿼리 읽는 법]
//  1. LEFT JOIN ( ... ) l : audit 에서 로그인 성공(action_id = 2 , action_result = true)만 회원별로 묶어서(GROUP BY)
//                           가장 늦은 시간(MAX) = 마지막 로그인 시간 → l.last_login_at
//                           LEFT JOIN 이라서 로그인 기록이 없는 회원도 빠지지 않음 (그 회원은 last_login_at 이 NULL)
//  2. ORDER BY CASE WHEN :loginSort = 'asc' THEN 시간 END ASC
//       → loginSort 가 'asc' 일 때만 시간으로 오름차순 , 아니면 모든 줄이 NULL 이라 순서에 영향 없음 (desc 도 같은 방식)
//       → 쿼리 하나로 "정렬 없음 / 오름차순 / 내림차순" 3가지를 다 처리
//       (MySQL 은 NULL 을 가장 작은 값으로 봐서 , 로그인 기록이 없는 회원은 오름차순이면 맨 앞 , 내림차순이면 맨 뒤)
//  3. 마지막 m.created_at DESC , m.member_id : 정렬 기준이 같으면 최근 가입순 → 그래도 같으면 회원 번호순
//       (순서가 매번 같아야 페이지를 넘길 때 같은 회원이 두 번 나오거나 빠지지 않음)
//
//  [페이징] 감사 로그(AuditRepository.search)와 같음 : Pageable 을 받으면 스프링이 끝에 LIMIT / OFFSET 을 붙이고 ,
//           countQuery 로 전체 인원을 세서 Page 에 같이 담아 줌 (인원 셀 때는 정렬 · 로그인 시간이 필요 없어서 뺌)
//           WHERE 조건은 두 쿼리가 똑같아야 함 → 필터 조건을 바꿀 때는 두 곳을 같이 고칠 것
//  ※ 회원의 role 은 LAZY 라서 DTO 로 바꿀 때 따로 조회되지만 , role 은 2종류뿐이라 많아야 2번 (같은 번호는 한 번만 조회)
//  ⚠️ 네이티브 쿼리는 테이블 · 컬럼 이름을 틀려도 서버가 켜질 때 알려주지 않음 → 사용자 목록을 열 때 오류가 남
    @Query(value = "SELECT m.* FROM member m " +
                   "JOIN role r ON r.role_id = m.role_id " +
                   "LEFT JOIN (SELECT member_id, MAX(created_at) AS last_login_at FROM audit " +
                   "           WHERE action_id = 2 AND action_result = true GROUP BY member_id) l " +
                   "       ON l.member_id = m.member_id " +
                   "WHERE m.member_id NOT IN ('GUEST', 'WITHDRAWN') " +
                   "AND (:roleName IS NULL OR r.role_name = :roleName) " +
                   "AND (:status IS NULL OR m.status = :status) " +
                   "ORDER BY CASE WHEN :loginSort = 'asc' THEN l.last_login_at END ASC, " +
                   "         CASE WHEN :loginSort = 'desc' THEN l.last_login_at END DESC, " +
                   "         m.created_at DESC, m.member_id ASC",
           countQuery = "SELECT COUNT(*) FROM member m " +
                        "JOIN role r ON r.role_id = m.role_id " +
                        "WHERE m.member_id NOT IN ('GUEST', 'WITHDRAWN') " +
                        "AND (:roleName IS NULL OR r.role_name = :roleName) " +
                        "AND (:status IS NULL OR m.status = :status)",
           nativeQuery = true)
    Page<MemberEntity> searchPage(@Param("roleName") String roleName, @Param("status") Boolean status,
                                  @Param("loginSort") String loginSort, Pageable pageable);

//  사용자 수 세기 (요약 카드용 , 필터와 상관없이 전체 기준 , 공통 계정 GUEST · WITHDRAWN 제외)
//  SELECT COUNT(*) FROM member WHERE member_id NOT IN ( ? , ? )
    long countByMemberIdNotIn(List<String> memberIds);

//  SELECT COUNT(*) FROM member WHERE status = ? AND member_id NOT IN ( ? , ? )
    long countByStatusAndMemberIdNotIn(Boolean status, List<String> memberIds);
}
