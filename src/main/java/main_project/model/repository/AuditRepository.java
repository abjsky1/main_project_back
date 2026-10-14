package main_project.model.repository;

import java.time.LocalDateTime;

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

//  ※ 회원별 최근 로그인 시간(예전 findLastLoginList)은 사용자 목록 쿼리(MemberRepository.searchPage)가 직접 계산함
//    → 최근 로그인순으로 정렬하려면 페이지를 나누기 전에 DB 에서 같이 계산해야 해서 그쪽으로 옮김

//  감사 로그 목록 조회 — 한 페이지씩 (필터 조건 + 최신순) + 사용자 , 회원 유형 , 작업 유형까지 한 번에
//  (화면에서는 각 조건이 표의 해당 컬럼 바로 위 필터 줄에 있음)
//  - user       : 이메일 또는 이름에 포함된 글자 (비회원은 이름이 '비회원')
//  - memberType : 회원 유형 이름 (관리자 / 수출입기업 / 물류운송업체 / 비회원 — signup 테이블의 signup_type 과 정확히 같은 것만)
//  - actionId   : 작업 유형 번호 (선택칸에서 고른 action_id — 이름 글자 검색이 아니라 번호가 정확히 같은 것만)
//  - ip         : 접속 IP 주소에 포함된 글자 (예: '203.0' → 203.0 으로 시작하거나 중간에 들어 있는 IP)
//  - result     : true(성공) / false(실패)
//  - from       : 이 시각 이후의 로그만 (기간 필터 — 예: 7일 전)
//  → 조건 값이 null 이면 그 조건은 빼고 조회 (전부 null 이면 전체)
//  memberEntity , signupEntity , actionEntity 모두 LAZY 라서 JOIN FETCH 로 같이 가져와야 쿼리 1번으로 끝 (N+1 방지).
//  회원 유형으로 거르려고 signupEntity 에 별명 s 를 붙임 (m = 회원 , t = 작업 유형 과 같은 방식)
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
                   "JOIN FETCH m.signupEntity s " +
                   "JOIN FETCH a.actionEntity t " +
                   "WHERE (:user IS NULL OR m.userEmail LIKE CONCAT('%', :user, '%') OR m.managerName LIKE CONCAT('%', :user, '%')) " +
                   "AND (:memberType IS NULL OR s.signupType = :memberType) " +
                   "AND (:actionId IS NULL OR t.actionId = :actionId) " +
                   "AND (:ip IS NULL OR a.fipAddress LIKE CONCAT('%', :ip, '%')) " +
                   "AND (:result IS NULL OR a.actionResult = :result) " +
                   "AND (:from IS NULL OR a.createdAt >= :from) " +
                   "ORDER BY a.createdAt DESC, a.auditId DESC",
           countQuery = "SELECT COUNT(a) FROM AuditEntity a " +
                        "JOIN a.memberEntity m " +
                        "JOIN m.signupEntity s " +
                        "JOIN a.actionEntity t " +
                        "WHERE (:user IS NULL OR m.userEmail LIKE CONCAT('%', :user, '%') OR m.managerName LIKE CONCAT('%', :user, '%')) " +
                        "AND (:memberType IS NULL OR s.signupType = :memberType) " +
                        "AND (:actionId IS NULL OR t.actionId = :actionId) " +
                        "AND (:ip IS NULL OR a.fipAddress LIKE CONCAT('%', :ip, '%')) " +
                        "AND (:result IS NULL OR a.actionResult = :result) " +
                        "AND (:from IS NULL OR a.createdAt >= :from)")
    Page<AuditEntity> search(@Param("user") String user, @Param("memberType") String memberType,
                             @Param("actionId") Integer actionId, @Param("ip") String ip, @Param("result") Boolean result,
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
