package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.Trade_typeEntity;

@Repository 
public interface Trade_typeRepository extends JpaRepository<Trade_typeEntity, Integer>{

}
