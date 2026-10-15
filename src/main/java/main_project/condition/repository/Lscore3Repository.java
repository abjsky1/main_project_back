package main_project.condition.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.condition.entity.Lscore1Entity;
import main_project.condition.entity.Lscore3Entity;

@Repository 
public interface Lscore3Repository extends JpaRepository<Lscore3Entity,Integer>{

    // Lscore1에 연결된 Lscore3 조회
    Optional<Lscore3Entity> findByLscore1Entity(Lscore1Entity lscore1Entity);
    
}
