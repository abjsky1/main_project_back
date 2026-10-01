package main_project.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.Cscore2Entity;

@Repository 
public interface Cscore2Repository extends JpaRepository<Cscore2Entity,Integer>{

    // Cscore1에 연결된 Cscore2 조회
    Optional<Cscore2Entity> findByCscore1Entity(Cscore1Entity cscore1Entity);

}
