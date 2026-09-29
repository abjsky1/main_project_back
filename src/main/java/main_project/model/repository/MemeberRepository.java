package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.MemberEntity;


@Repository 
public interface MemeberRepository extends JpaRepository <MemberEntity , Integer> {

}
