package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.Cscore3Repository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class Cscore3Service {

    private final Cscore3Repository cscore3Repository;




    
}
