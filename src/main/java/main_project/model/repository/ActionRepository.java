package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import main_project.model.entity.ActionEntity;

public interface ActionRepository extends JpaRepository < ActionEntity , Integer > {

}
