package main_project.insight.repository;

import main_project.insight.entity.InsightHistoryEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsightHistoryRepository extends JpaRepository<InsightHistoryEntity, Integer> {

    // [1] 본인 기록을 최신순으로 조회
    Page<InsightHistoryEntity>
    findByMemberIdOrderByCreatedAtDescInsightIdDesc(String memberId, Pageable pageable);

    // [2] 기록 번호와 회원 ID로 본인 기록 조회
    Optional<InsightHistoryEntity> findByInsightIdAndMemberId(Integer insightId,String memberId);
}