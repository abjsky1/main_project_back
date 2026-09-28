package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.MemberTypeRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class MemberTypeService {

    private final MemberTypeRepository memberTypeRepository;





}
