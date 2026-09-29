package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.RoleRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class RoleService {

    private final RoleRepository roleRepository;





}
