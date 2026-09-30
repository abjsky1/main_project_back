package main_project.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import main_project.model.entity.AuditEntity;
import main_project.model.entity.MemberEntity;

@Repository 
public interface AuditRepository extends JpaRepository<AuditEntity,Integer>{

    // 특정 회원의 로그인 성공(action_id=2, action_result=true) 기록 중 가장 최근(최신 createdAt) 1건을 조회
    // 쿼리 메서드 혹은 @Query 사용 가능
    @Query("SELECT a FROM AuditEntity a WHERE a.memberEntity = :member AND a.actionEntity.actionId = 2 AND a.actionResult = true ORDER BY a.createdAt DESC LIMIT 1")
    Optional<MemberEntity> findTopByMemberEntityAndLoginSuccessOrderByCreatedAtDesc( @Param("member") MemberEntity member );

}
