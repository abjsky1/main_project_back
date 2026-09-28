package main_project.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import main_project.model.repository.MemberTypeRepository;

@Service 
@RequiredArgsConstructor 
public class MemberTypeService {

    private final MemberTypeRepository memberTypeRepository;




    
}
