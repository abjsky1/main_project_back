package main_project.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.Lscore2Entity;



@Repository 
public interface Lscore2epository extends JpaRepository<Lscore2Entity,Integer>{

    // Lscore1에 연결된 Lscore2 조회
    Optional<Lscore2Entity> findByLscore1Entity(Lscore1Entity lscore1Entity);
    
    // Lscore1에 연결된 Lscore2 삭제

}
