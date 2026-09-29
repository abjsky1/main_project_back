package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import main_project.model.repository.SignupRepository;

@Service 
@RequiredArgsConstructor 
public class SignupService {

    private final SignupRepository signupRepository;





}
