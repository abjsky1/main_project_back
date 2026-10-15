package main_project.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.audit.entity.ActionEntity;


@Repository 
public interface ActionRepository extends JpaRepository < ActionEntity , Integer > {

}
