package main_project.condition.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.condition.entity.Lscore1Entity;
import main_project.condition.entity.Lscore2Entity;



@Repository 
public interface Lscore2Repository extends JpaRepository<Lscore2Entity,Integer>{

    // Lscore1에 연결된 Lscore2 조회
    Optional<Lscore2Entity> findByLscore1Entity(Lscore1Entity lscore1Entity);

}
