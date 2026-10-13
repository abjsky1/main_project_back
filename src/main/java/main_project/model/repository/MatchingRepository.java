
package main_project.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
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

    // 특정 물류기업 회원의 매칭 목록 조회
    List<MatchingEntity> findByLscore1EntityMemberEntityMemberId(String memberId);

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

    // =====================================================================
    // [8] ~ [9] 관리자 매칭 관리 화면 (상태별 건수 5개 + 고른 상태의 목록 한 페이지)
    //  - 상태 4가지 (예전 [8] ~ [11] 상태별 목록 쿼리 4개의 조건을 그대로 가져와서 2개의 쿼리로 합침)
    //      linked    자동 매칭 연결 : 화주 WAITING  , 운송사 WAITING , 최종 PENDING (추천만 생성 , 둘 다 응답 전)
    //      requested 물류 수락 대기 : 화주 ACCEPTED , 운송사 WAITING , 최종 PENDING (화주는 요청 , 운송사 응답 전)
    //      completed 매칭 성사     : 최종 COMPLETED
    //      failed    매칭 실패     : 최종 FAILED (화주 또는 운송사 거절)
    //  ⚠️ 상태 조건은 [8] 건수 쿼리와 [9] 목록 쿼리에 똑같이 들어 있음 → 조건을 바꿀 때는 두 곳을 같이 고칠 것
    // =====================================================================

    // [8] 결과를 담을 모양 (AuditRepository 의 LastLogin 과 같은 방식)
    //  getter 이름 = 아래 쿼리의 AS 별칭 → Spring 이 알아서 값을 채워 줌 (getAllCount( ) 로 바로 꺼내 씀)
    interface StatusCount {
        Long getAllCount();         // 전체
        Long getLinkedCount();      // 자동 매칭 연결
        Long getRequestedCount();   // 물류 수락 대기
        Long getCompletedCount();   // 매칭 성사
        Long getFailedCount();      // 매칭 실패
    }

    // [8] 상태별 건수 5개를 쿼리 1번으로 한꺼번에 세기
    //  - COUNT(m) : 전체 행 수
    //  - SUM(CASE WHEN 조건 THEN 1 ELSE 0 END) : 조건에 맞는 행은 1 , 아니면 0 으로 바꿔서 더함 = 조건에 맞는 행 수
    //      → 상태마다 COUNT 쿼리를 따로 4번 보내지 않고 , 표를 한 번만 훑으면서 4가지를 동시에 셈
    //  - COALESCE( 값 , 0 ) : 매칭이 하나도 없으면 SUM 결과가 null 이라서 0 으로 바꿈
    //  (SQL 로 치면) SELECT COUNT(*) , SUM(CASE WHEN ... THEN 1 ELSE 0 END) , ... FROM matching
    @Query("SELECT COUNT(m) AS allCount, " +
           "COALESCE(SUM(CASE WHEN m.shipperStatus = 'WAITING' AND m.logisticsStatus = 'WAITING' AND m.finalStatus = 'PENDING' THEN 1 ELSE 0 END), 0) AS linkedCount, " +
           "COALESCE(SUM(CASE WHEN m.shipperStatus = 'ACCEPTED' AND m.logisticsStatus = 'WAITING' AND m.finalStatus = 'PENDING' THEN 1 ELSE 0 END), 0) AS requestedCount, " +
           "COALESCE(SUM(CASE WHEN m.finalStatus = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedCount, " +
           "COALESCE(SUM(CASE WHEN m.finalStatus = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedCount " +
           "FROM MatchingEntity m")
    StatusCount findStatusCount();

    // [9] 고른 상태의 매칭 목록 한 페이지 (최신순)
    //  - status : all / linked / requested / completed / failed (Service 가 이 5가지 중 하나로 정리해서 넘김)
    //      (:status = 'all' OR ...) : AuditRepository 의 (:user IS NULL OR ...) 처럼 , 고른 상태의 조건만 살아남게 하는 방식
    //  - Pageable : "몇 번째 페이지를 50건씩" → 스프링이 쿼리 끝에 LIMIT / OFFSET 을 붙여서 그 페이지만 가져옴
    //  - 반환 타입이 Page 가 아니라 List 인 이유 : 전체 건수는 [8] 에서 이미 셌으니 COUNT 쿼리를 또 보낼 필요가 없음
    //      (Page 로 받으면 스프링이 COUNT 쿼리를 자동으로 한 번 더 실행함)
    //  - JOIN FETCH : 표에 화주 · 운송사 회사 정보가 필요해서 매칭 + 화주 조건 + 화주 회원 + 운송사 조건 + 운송사 회원을 쿼리 1번에 같이 가져옴
    //      → 안 붙이면 50건을 그릴 때 회사 정보를 한 건씩 따로 조회해서 쿼리가 수십 번 나감 (N+1 문제)
    @Query("SELECT m FROM MatchingEntity m " +
           "JOIN FETCH m.cscore1Entity c JOIN FETCH c.memberEntity " +
           "JOIN FETCH m.lscore1Entity l JOIN FETCH l.memberEntity " +
           "WHERE (:status = 'all') " +
           "OR (:status = 'linked' AND m.shipperStatus = 'WAITING' AND m.logisticsStatus = 'WAITING' AND m.finalStatus = 'PENDING') " +
           "OR (:status = 'requested' AND m.shipperStatus = 'ACCEPTED' AND m.logisticsStatus = 'WAITING' AND m.finalStatus = 'PENDING') " +
           "OR (:status = 'completed' AND m.finalStatus = 'COMPLETED') " +
           "OR (:status = 'failed' AND m.finalStatus = 'FAILED') " +
           "ORDER BY m.createdAt DESC, m.matchingId DESC")
    List<MatchingEntity> findAdminPage(@Param("status") String status, Pageable pageable);

}
