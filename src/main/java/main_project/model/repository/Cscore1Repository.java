package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Cscore1Entity;

@Repository 
public interface Cscore1Repository extends JpaRepository<Cscore1Entity,Long>{

}
