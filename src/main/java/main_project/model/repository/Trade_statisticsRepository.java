package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import main_project.model.entity.Trade_statisticsEntity;

public interface Trade_statisticsRepository extends JpaRepository<Trade_statisticsEntity,Integer>{

}
