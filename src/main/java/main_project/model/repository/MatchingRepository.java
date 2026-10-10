
package main_project.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import main_project.model.entity.MatchingEntity;

@Repository
public interface MatchingRepository extends JpaRepository<MatchingEntity, Integer> {


    // [1] 화주 매칭 조건 번호로 매칭 결과 조회
    // 화주 매칭 조건 삭제 시 사용
    List<MatchingEntity> findByCscore1EntityCscore1Id(Integer cscore1Id);


    // [2] 물류기업 매칭 조건 번호로 매칭 결과 조회
    // 물류기업 매칭 조건 삭제 시 사용
    List<MatchingEntity> findByLscore1EntityLscore1Id(Integer lscore1Id);


    // [3] 동일한 화주 조건과 물류기업 조건의 매칭이 존재하는지 확인
    // 자동 매칭 시 중복 추천 방지
    boolean existsByCscore1EntityCscore1IdAndLscore1EntityLscore1Id(
            Integer cscore1Id,
            Integer lscore1Id
    );


    // [4] 전체 매칭 결과 최신순 조회
    // 관리자 매칭 목록에서 사용
    List<MatchingEntity> findAllByOrderByCreatedAtDescMatchingIdDesc();


    // [5] 로그인한 회원의 매칭 결과 조회
    // 화주는 본인의 모든 추천 매칭 조회
    // 물류기업은 화주가 요청한 매칭만 조회
    @Query("SELECT m FROM MatchingEntity m " +
           "WHERE m.cscore1Entity.memberEntity.memberId = :memberId " +
           "OR (m.lscore1Entity.memberEntity.memberId = :memberId " +
           "AND m.shipperStatus = 'ACCEPTED') " +
           "ORDER BY m.totalScore DESC, m.matchingId ASC")
    List<MatchingEntity> findVisibleToMember(@Param("memberId") String memberId);


    // [6] 같은 화주 조건으로 이미 진행 중이거나 완료된 매칭 조회
    // 여러 물류기업에 동시에 매칭 요청하는 것을 방지
    @Query("SELECT m FROM MatchingEntity m " +
           "WHERE m.cscore1Entity.cscore1Id = :cscore1Id " +
           "AND (m.finalStatus = 'COMPLETED' " +
           "OR (m.finalStatus = 'PENDING' " +
           "AND m.shipperStatus = 'ACCEPTED'))")
    List<MatchingEntity> findBlockingRequests(@Param("cscore1Id") Integer cscore1Id);


    // [7] 매칭 상태 변경을 위한 조회 및 DB 잠금
    // 화주 요청·거절, 물류기업 수락·거절 시 사용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MatchingEntity> findByMatchingId(Integer matchingId);

}
