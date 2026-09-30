package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Cscore3Entity;

@Repository 
public interface Cscore3Repository extends JpaRepository<Cscore3Entity,Integer>{

}
