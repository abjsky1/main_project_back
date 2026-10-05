package main_project.model.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import main_project.model.entity.MemberEntity;


@Repository 
public interface MemberRepository extends JpaRepository <MemberEntity , String> {


// 로그인용 회원 조회
Optional<MemberEntity> findByUserEmail(String userEmail);

//  사용자 권한 관리 목록 조회 (필터 조건 + 역할(role)까지 한 번에)
//  - roleName , status 가 null 이면 그 조건은 빼고 조회 (둘 다 null 이면 전체)
//  - roleEntity 는 LAZY 라서 JOIN FETCH 로 같이 가져와야 회원마다 role 쿼리가 추가로 안 나감 (쿼리 1번)
//  - 감사 로그용 비회원 공통 계정(GUEST , AuditService.GUEST_MEMBER_ID)은 실제 회원이 아니라서 제외
    @Query("SELECT m FROM MemberEntity m JOIN FETCH m.roleEntity r " +
           "WHERE m.memberId <> 'GUEST' " +
           "AND (:roleName IS NULL OR r.roleName = :roleName) " +
           "AND (:status IS NULL OR m.status = :status)")
    List<MemberEntity> searchWithRole(@Param("roleName") String roleName, @Param("status") Boolean status);

//  사용자 수 세기 (요약 카드용 , 필터와 상관없이 전체 기준 , GUEST 제외)
//  SELECT COUNT(*) FROM member WHERE member_id <> ?
    long countByMemberIdNot(String memberId);

//  SELECT COUNT(*) FROM member WHERE status = ? AND member_id <> ?
    long countByStatusAndMemberIdNot(Boolean status, String memberId);
}
