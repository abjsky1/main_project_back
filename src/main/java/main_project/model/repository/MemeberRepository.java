package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import main_project.model.entity.MemberEntity;
import main_project.model.entity.MemberRoleEntity;

public interface MemeberRepository extends JpaRepository <MemberEntity , Integer> {

}
