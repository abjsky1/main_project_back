package main_project.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import main_project.model.repository.ActionRepository;

@Service 
@RequiredArgsConstructor 
public class ActionService {

    private final ActionRepository actionRepository;





}
