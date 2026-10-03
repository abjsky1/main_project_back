package main_project.model.repository;

import java.util.Optional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore2Entity;
import main_project.model.entity.Cscore3Entity;

@Repository 
public interface Cscore1Repository extends JpaRepository<Cscore1Entity,Integer>{

    // 회원별 매칭 조건 조회
    List<Cscore1Entity> findByMemberEntityMemberId(String memberId);
}
