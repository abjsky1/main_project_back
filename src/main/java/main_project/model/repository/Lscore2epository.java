package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Lscore2Entity;

@Repository 
public interface Lscore2epository extends JpaRepository<Lscore2Entity,Long>{

}
