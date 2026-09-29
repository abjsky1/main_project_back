package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.MatchingRepository;


@Service 
@RequiredArgsConstructor 
public class MatchingService {

    private final MatchingRepository matchingRepository;







}
