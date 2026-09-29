package main_project.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import main_project.model.repository.AuditRepository;

@Service 
@RequiredArgsConstructor 
public class AuditService {

    private final AuditRepository auditRepository;






    
}
