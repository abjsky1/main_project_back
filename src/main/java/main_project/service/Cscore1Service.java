package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.Cscore1Repository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class Cscore1Service {

    private final Cscore1Repository cscore1Repository;





    
}
