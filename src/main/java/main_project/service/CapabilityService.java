package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.CapabilityRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class CapabilityService {

    private final CapabilityRepository capabilityRepository;




    
}
