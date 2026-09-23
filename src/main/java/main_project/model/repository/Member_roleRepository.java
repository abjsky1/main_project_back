package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Member_roleEntity;
@Repository 
public interface Member_roleRepository extends JpaRepository<Member_roleEntity , Integer> {

}
