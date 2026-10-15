package main_project.insight.repository;

import main_project.insight.entity.InsightHistoryEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsightHistoryRepository extends JpaRepository<InsightHistoryEntity, Integer> {

    Page<InsightHistoryEntity>
    findByMember_MemberIdOrderByCreatedAtDescInsightIdDesc(String memberId, Pageable pageable);

    Optional<InsightHistoryEntity>
    findByInsightIdAndMember_MemberId(Integer insightId, String memberId);
}