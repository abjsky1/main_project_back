package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.AuditLogRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;







}
