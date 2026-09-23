package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Member_typeEntity;

@Repository 
public interface Member_typeRepository extends JpaRepository<Member_typeEntity , Integer> {

}
