package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.RequestRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class RequestService {

    private final RequestRepository requestRepository;


    

}
