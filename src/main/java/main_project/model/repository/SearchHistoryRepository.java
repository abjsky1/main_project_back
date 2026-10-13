package main_project.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import main_project.model.entity.SearchHistoryEntity;

public interface SearchHistoryRepository extends JpaRepository<SearchHistoryEntity, Integer> {
}
