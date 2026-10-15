package main_project.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import main_project.trade.entity.SearchHistoryEntity;

public interface SearchHistoryRepository extends JpaRepository<SearchHistoryEntity, Integer> {
}
