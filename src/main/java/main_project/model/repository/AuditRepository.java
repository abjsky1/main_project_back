package main_project.model.repository;

import java.time.LocalDateTime;
import java.util.List;

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

//  회원별 최근 로그인 성공 시간 한 번에 조회
//  action_id = 2 (로그인) , action_result = true (성공) 인 로그만 남기고
//  member_id 별로 묶어서(GROUP BY) 그 중 가장 늦은 시간(MAX)을 가져옴.
//  → 회원이 100명이어도 쿼리는 딱 1번.
//
//  nativeQuery 가 아닌 JPQL : 테이블명 대신 엔티티명(AuditEntity) , 컬럼명 대신 필드명(createdAt) 을 씀.
//  엔티티 필드 타입 그대로 LocalDateTime 으로 받아와서 형변환 걱정이 없음.
//  (네이티브 쿼리로 치면 아래와 같음)
//  SELECT member_id AS memberId, MAX(created_at) AS lastLoginAt FROM audit
//  WHERE action_id = 2 AND action_result = true GROUP BY member_id
    @Query("SELECT a.memberEntity.memberId AS memberId, MAX(a.createdAt) AS lastLoginAt " +
           "FROM AuditEntity a " +
           "WHERE a.actionEntity.actionId = 2 AND a.actionResult = true " +
           "GROUP BY a.memberEntity.memberId")

//  @Query("SELECT member_id AS memberId, MAX(created_at) AS lastLoginAt FROM audit WHERE action_id = 2 AND action_result = true GROUP BY member_id", nativeQuery = true )
    List<LastLogin> findLastLoginList();

//  감사 로그 목록 조회 (필터 조건 + 최신순) + 사용자 , 회원 유형 , 작업 유형까지 한 번에
//  - user   : 이메일 또는 이름에 포함된 글자 (비회원은 이름이 '비회원')
//  - action : 작업 유형 이름에 포함된 글자
//  - result : true(성공) / false(실패)
//  → 조건 값이 null 이면 그 조건은 빼고 조회 (전부 null 이면 전체)
//  memberEntity , signupEntity , actionEntity 모두 LAZY 라서 JOIN FETCH 로 같이 가져와야 쿼리 1번으로 끝 (N+1 방지).
//  ORDER BY : 최신 로그가 위로 (같은 시간이면 나중에 저장된 번호가 위로)
    @Query("SELECT a FROM AuditEntity a " +
           "JOIN FETCH a.memberEntity m " +
           "JOIN FETCH m.signupEntity " +
           "JOIN FETCH a.actionEntity t " +
           "WHERE (:user IS NULL OR m.userEmail LIKE CONCAT('%', :user, '%') OR m.managerName LIKE CONCAT('%', :user, '%')) " +
           "AND (:action IS NULL OR t.actionType LIKE CONCAT('%', :action, '%')) " +
           "AND (:result IS NULL OR a.actionResult = :result) " +
           "ORDER BY a.createdAt DESC, a.auditId DESC")
    List<AuditEntity> search(@Param("user") String user, @Param("action") String action, @Param("result") Boolean result);

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
