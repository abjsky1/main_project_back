package main_project.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.Lscore3Entity;

@Repository 
public interface Lscore3Repository extends JpaRepository<Lscore3Entity,Integer>{

    // Lscore1에 연결된 Lscore3 조회
    Optional<Lscore3Entity> findByLscore1Entity(Lscore1Entity lscore1Entity);
    
}
