package main_project.model.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import main_project.model.entity.AuditEntity;


@Repository 
public interface AuditRepository extends JpaRepository<AuditEntity,Integer>{

//  [조회 결과를 담을 모양]
//  Map<String,Object> 대신 이렇게 getter 이름만 적어두면 Spring 이 알아서 값을 채워줌.
//  → map.get("memberId") 처럼 문자열 키 + 형변환 할 필요 없이 getMemberId() 로 바로 꺼내 쓸 수 있음.
//  (getter 이름은 아래 쿼리의 AS 별칭과 똑같아야 함 : memberId , lastLoginAt)
    interface LastLogin {
        String getMemberId();
        LocalDateTime getLastLoginAt();
    }

//  회원별 최근 로그인 성공 시간 한 번에 조회 — 사용자 목록의 "이번 페이지 회원들" 만
//  action_id = 2 (로그인) , action_result = true (성공) 인 로그만 남기고
//  member_id 별로 묶어서(GROUP BY) 그 중 가장 늦은 시간(MAX)을 가져옴.
//  → 회원이 50명이어도 쿼리는 딱 1번.
//  memberIds : 이번 페이지 회원 번호 목록 (IN : 목록 안에 있는 회원만 — 전체 회원의 로그인 시간을 다 셀 필요 없음)
//
//  nativeQuery 가 아닌 JPQL : 테이블명 대신 엔티티명(AuditEntity) , 컬럼명 대신 필드명(createdAt) 을 씀.
//  엔티티 필드 타입 그대로 LocalDateTime 으로 받아와서 형변환 걱정이 없음.
//  (네이티브 쿼리로 치면 아래와 같음)
//  SELECT member_id AS memberId, MAX(created_at) AS lastLoginAt FROM audit
//  WHERE action_id = 2 AND action_result = true AND member_id IN ( ? , ? , ... ) GROUP BY member_id
    @Query("SELECT a.memberEntity.memberId AS memberId, MAX(a.createdAt) AS lastLoginAt " +
           "FROM AuditEntity a " +
           "WHERE a.actionEntity.actionId = 2 AND a.actionResult = true " +
           "AND a.memberEntity.memberId IN :memberIds " +
           "GROUP BY a.memberEntity.memberId")
    List<LastLogin> findLastLoginList(@Param("memberIds") List<String> memberIds);

//  감사 로그 목록 조회 — 한 페이지씩 (필터 조건 + 최신순) + 사용자 , 회원 유형 , 작업 유형까지 한 번에
//  - user   : 이메일 또는 이름에 포함된 글자 (비회원은 이름이 '비회원')
//  - action : 작업 유형 이름에 포함된 글자
//  - result : true(성공) / false(실패)
//  - from   : 이 시각 이후의 로그만 (기간 필터 — 예: 7일 전)
//  → 조건 값이 null 이면 그 조건은 빼고 조회 (전부 null 이면 전체)
//  memberEntity , signupEntity , actionEntity 모두 LAZY 라서 JOIN FETCH 로 같이 가져와야 쿼리 1번으로 끝 (N+1 방지).
//  ORDER BY : 최신 로그가 위로 (같은 시간이면 나중에 저장된 번호가 위로)
//
//  [페이징] 마지막 파라미터 Pageable + 반환 타입 Page
//  - Pageable : "몇 번째 페이지를 몇 건씩" 이라는 요청 (Service 에서 PageRequest.of(페이지번호 , 50) 로 만들어서 넘김)
//  - 스프링이 위 쿼리 끝에 LIMIT / OFFSET 을 자동으로 붙여서 그 페이지만 가져옴
//      예) 3페이지(번호 2) , 50건씩 → ... ORDER BY ... LIMIT 50 OFFSET 100   (101번째부터 50건)
//  - Page : 그 페이지의 로그 목록 + 전체 건수 · 전체 페이지 수를 같이 담은 상자
//      → 전체 건수를 알아야 페이지 버튼을 몇 개 그릴지 알 수 있어서 , 스프링이 COUNT 쿼리를 한 번 더 실행함
//
//  [countQuery] 전체 건수를 세는 쿼리를 직접 적은 것
//  - 원래는 스프링이 위 쿼리를 보고 COUNT 쿼리를 알아서 만드는데 , JOIN FETCH 가 있으면 COUNT 쿼리를 못 만들어서 오류가 남
//    (개수만 셀 때는 회원 · 작업 정보를 같이 가져올 필요가 없어서 "FETCH" 를 뺀 JOIN 으로)
//  - WHERE 조건은 위 쿼리와 똑같아야 함 → 필터 조건을 바꿀 때는 두 곳을 같이 고칠 것
    @Query(value = "SELECT a FROM AuditEntity a " +
                   "JOIN FETCH a.memberEntity m " +
                   "JOIN FETCH m.signupEntity " +
                   "JOIN FETCH a.actionEntity t " +
                   "WHERE (:user IS NULL OR m.userEmail LIKE CONCAT('%', :user, '%') OR m.managerName LIKE CONCAT('%', :user, '%')) " +
                   "AND (:action IS NULL OR t.actionType LIKE CONCAT('%', :action, '%')) " +
                   "AND (:result IS NULL OR a.actionResult = :result) " +
                   "AND (:from IS NULL OR a.createdAt >= :from) " +
                   "ORDER BY a.createdAt DESC, a.auditId DESC",
           countQuery = "SELECT COUNT(a) FROM AuditEntity a " +
                        "JOIN a.memberEntity m " +
                        "JOIN a.actionEntity t " +
                        "WHERE (:user IS NULL OR m.userEmail LIKE CONCAT('%', :user, '%') OR m.managerName LIKE CONCAT('%', :user, '%')) " +
                        "AND (:action IS NULL OR t.actionType LIKE CONCAT('%', :action, '%')) " +
                        "AND (:result IS NULL OR a.actionResult = :result) " +
                        "AND (:from IS NULL OR a.createdAt >= :from)")
    Page<AuditEntity> search(@Param("user") String user, @Param("action") String action, @Param("result") Boolean result,
                             @Param("from") LocalDateTime from, Pageable pageable);

//  회원 1명의 감사 로그를 다른 회원(탈퇴 회원 공통 계정)으로 한 번에 옮기기 — 회원 탈퇴에서 사용
//  - 로그가 몇 건이든 UPDATE 쿼리 1번 (로그를 자바로 가져와서 한 건씩 바꾸면 로그 수만큼 UPDATE 가 실행됨)
//  - nativeQuery = true : MySQL 쿼리를 그대로 씀 → 엔티티 이름(AuditEntity) 대신 테이블 이름(audit) ,
//                         필드 이름(memberEntity) 대신 컬럼 이름(member_id)
//  - :toMemberId , :fromMemberId : 아래 @Param("이름") 과 같은 이름의 값이 들어가는 자리 (두 이름이 똑같아야 연결됨)
//                                  문자열을 직접 이어 붙이지 않아서 SQL 인젝션도 막아 줌
//  - @Modifying : SELECT 가 아니라 데이터를 바꾸는 쿼리라는 표시 (UPDATE / DELETE 쿼리에 꼭 필요)
//  - 돌려주는 int : 바뀐 줄 수 (= 옮긴 로그 건수)
//  ⚠️ 네이티브 쿼리는 테이블 · 컬럼 이름을 틀려도 서버가 켜질 때 알려주지 않음 → 이 쿼리가 실행될 때(회원 탈퇴할 때) 오류가 남
    @Modifying
    @Query(value = "UPDATE audit SET member_id = :toMemberId WHERE member_id = :fromMemberId", nativeQuery = true)
    int moveLogs(@Param("fromMemberId") String fromMemberId, @Param("toMemberId") String toMemberId);

}
