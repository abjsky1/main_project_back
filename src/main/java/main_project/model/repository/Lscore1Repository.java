package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Lscore1Entity;

@Repository 
public interface Lscore1Repository extends JpaRepository<Lscore1Entity,Long>{

}
