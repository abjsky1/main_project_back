
package main_project.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Lscore1Entity;

@Repository
public interface Lscore1Repository extends JpaRepository<Lscore1Entity,Integer>{

    // [1] 회원별 물류기업 매칭 조건 조회
    List<Lscore1Entity> findByMemberEntityMemberId(String memberId);


    // [2] 매칭에 동의한 물류기업 조건만 조회 (추가)
    List<Lscore1Entity> findByMatchingAgreeTrue();

}
