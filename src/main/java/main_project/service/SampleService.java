package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.SampleRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class SampleService {

    private final SampleRepository sampleRepository;



    
}
