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


// [사용 금지] 이메일 평문으로 회원 조회
//  - user_email 칸은 암호화되어(매번 다른 암호문) 이 메소드는 항상 "없음"을 돌려준다
//  - 대신 findByUserEmailHash( hmacUtil.hash( PiiNormalizer.normalizeEmail( 이메일 ) ) ) 사용
//  - AuditRequestReader 가 아직 사용 중이라 컴파일 유지를 위해 남겨둠 → 바꾸면 삭제
@Deprecated
Optional<MemberEntity> findByUserEmail(String userEmail);

//  ---------- 암호화된 개인정보 검색 (HMAC 검색값 사용) ----------
//  - 넣는 값 : PiiNormalizer 로 정규화 → HmacUtil.hash( ) 로 만든 64글자 값
//  SELECT * FROM member WHERE user_email_hash = ?
Optional<MemberEntity> findByUserEmailHash(String userEmailHash);

//  SELECT * FROM member WHERE business_reg_no_hash = ?
Optional<MemberEntity> findByBusinessRegNoHash(String businessRegNoHash);

//  회원가입 중복 확인용 (있으면 true)
boolean existsByUserEmailHash(String userEmailHash);

boolean existsByBusinessRegNoHash(String businessRegNoHash);

//  사용자 권한 관리 목록 조회 (필터 조건 + 역할(role)까지 한 번에)
//  - roleName , status 가 null 이면 그 조건은 빼고 조회 (둘 다 null 이면 전체)
//  - roleEntity 는 LAZY 라서 JOIN FETCH 로 같이 가져와야 회원마다 role 쿼리가 추가로 안 나감 (쿼리 1번)
//  - 감사 로그용 공통 계정(비회원 GUEST , 탈퇴 회원 WITHDRAWN)은 실제 회원이 아니라서 제외
    @Query("SELECT m FROM MemberEntity m JOIN FETCH m.roleEntity r " +
           "WHERE m.memberId NOT IN ('GUEST', 'WITHDRAWN') " +
           "AND (:roleName IS NULL OR r.roleName = :roleName) " +
           "AND (:status IS NULL OR m.status = :status)")
    List<MemberEntity> searchWithRole(@Param("roleName") String roleName, @Param("status") Boolean status);

//  사용자 수 세기 (요약 카드용 , 필터와 상관없이 전체 기준 , 공통 계정 GUEST · WITHDRAWN 제외)
//  SELECT COUNT(*) FROM member WHERE member_id NOT IN ( ? , ? )
    long countByMemberIdNotIn(List<String> memberIds);

//  SELECT COUNT(*) FROM member WHERE status = ? AND member_id NOT IN ( ? , ? )
    long countByStatusAndMemberIdNotIn(Boolean status, List<String> memberIds);
}
