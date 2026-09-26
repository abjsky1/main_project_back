package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import main_project.model.entity.TradeTypeEntity;

@Repository 
public interface TradeTypeRepository extends JpaRepository<TradeTypeEntity, Integer>{

}
