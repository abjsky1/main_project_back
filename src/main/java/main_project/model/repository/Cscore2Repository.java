package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Cscore2Entity;

@Repository 
public interface Cscore2Repository extends JpaRepository<Cscore2Entity,Integer>{

}
