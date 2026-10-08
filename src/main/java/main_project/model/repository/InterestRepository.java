package main_project.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.InterestEntity;

@Repository
public interface InterestRepository extends JpaRepository<InterestEntity, Integer> {

    // 회원별 관심 국가 조회 (등록한 순서대로 = interest_id 오름차순)
    // SELECT * FROM interest WHERE member_id = ? ORDER BY interest_id ASC
    List<InterestEntity> findByMemberEntityMemberIdOrderByInterestIdAsc(String memberId);

}
