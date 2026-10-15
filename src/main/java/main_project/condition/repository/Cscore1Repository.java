
package main_project.condition.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import main_project.condition.entity.Cscore1Entity;

@Repository
public interface Cscore1Repository extends JpaRepository<Cscore1Entity, Integer> {

    // [1] 회원별 매칭 조건 조회 (기존 코드)
    List<Cscore1Entity> findByMemberEntityMemberId(String memberId);

    // [2] 매칭에 동의한 화주 조건만 조회 (추가)
    List<Cscore1Entity> findByMatchingAgreeTrue();

    // [추가] 새로 등록한 물류기업의 노선과 일치하는 화주 조건만 먼저 조회
    @Query("SELECT c FROM Cscore1Entity c JOIN FETCH c.memberEntity mem WHERE c.matchingAgree = true "
            + "AND mem.status = true "
            + "AND c.countryId = :countryId "
            + "AND c.tradeType = :tradeType "
            + "AND c.transportType = :transportType "
            + "AND c.departure = :departure "
            + "AND c.arrival = :arrival")
    List<Cscore1Entity> findMatchingCandidatesForLogistics(
            @Param("countryId") Integer countryId,
            @Param("tradeType") Boolean tradeType,
            @Param("transportType") Boolean transportType,
            @Param("departure") Integer departure,
            @Param("arrival") Integer arrival);

    // [3] 화주 조건 조회 및 DB 잠금 (추가)
    // 자동 매칭 및 매칭 요청 상태 변경 시 사용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cscore1Entity c WHERE c.cscore1Id = :cscore1Id")
    Optional<Cscore1Entity> findForUpdate(@Param("cscore1Id") Integer cscore1Id);

}
