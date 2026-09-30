package main_project.service;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationDto;
import main_project.model.repository.AuthorizationRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class AuthorizationService {

    private final AuthorizationRepository authorizationRepository;


//  사용자 권한 관리 사용자 목록 조회
    public List<AuthorizationDto> AuthorizationFindAll(){

        
    }











}
