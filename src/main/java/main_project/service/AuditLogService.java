package main_project.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import main_project.model.repository.AuditLogRepository;

@Service 
@RequiredArgsConstructor 
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;






    
}
