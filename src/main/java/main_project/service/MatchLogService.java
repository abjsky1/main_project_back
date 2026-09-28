package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.MatchLogRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class MatchLogService {

    private final MatchLogRepository matchLogRepository;







}
