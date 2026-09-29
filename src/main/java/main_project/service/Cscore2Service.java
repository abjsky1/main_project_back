package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.Cscore2Repository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class Cscore2Service {

    private final Cscore2Repository cscore2Repository;





    
}
