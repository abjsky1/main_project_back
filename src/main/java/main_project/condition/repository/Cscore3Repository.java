package main_project.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore2Entity;
import main_project.model.entity.Cscore3Entity;
import java.util.List;


@Repository 
public interface Cscore3Repository extends JpaRepository<Cscore3Entity,Integer>{

    // Cscore1에 연결된 Cscore3 조회
    Optional<Cscore3Entity> findByCscore1Entity(Cscore1Entity cscore1Entity);
    
}
