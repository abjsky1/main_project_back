package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.MemberRoleRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class MemberRoleService {

    private final MemberRoleRepository memberRoleRepository;






}
