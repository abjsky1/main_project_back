package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import main_project.model.repository.RoleRepository;

@Service 
@RequiredArgsConstructor 
public class TradeTypeService {

    private final RoleRepository roleRepository;






}
