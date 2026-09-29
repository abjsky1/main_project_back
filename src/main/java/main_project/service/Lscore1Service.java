package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.Lscore1Repository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class Lscore1Service {

    private final Lscore1Repository lscore1Repository;




    
}
