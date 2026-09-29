package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.Lscore3Repository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class Lscore3Service {

    private final Lscore3Repository lscore3Repository;






    
}
