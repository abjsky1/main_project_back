package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.Lscore2epository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class Lscore2Service {

    private final Lscore2epository lscore2epository;




    
}
