package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Lscore3Entity;

@Repository 
public interface Lscore3Repository extends JpaRepository<Lscore3Entity,Long>{

}
