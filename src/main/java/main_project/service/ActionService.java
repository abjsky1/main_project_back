package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.ActionRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class ActionService {

    private final ActionRepository actionRepository;





}
