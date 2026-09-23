package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Match_logEntity;

@Repository 
public interface Match_logRepository extends JpaRepository<Match_logEntity , Integer> {

}
