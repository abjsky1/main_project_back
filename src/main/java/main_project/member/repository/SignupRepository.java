package main_project.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.member.entity.SignupEntity;


@Repository 
public interface SignupRepository extends JpaRepository<SignupEntity , String> {

}
