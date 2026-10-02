package main_project.service;
 
import java.util.List;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuditDto;
import main_project.model.entity.AuditEntity;
import main_project.model.repository.AuditRepository;
 
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {
 
    private final AuditRepository auditRepository;
 
//  감사 로그 목록 조회 (최신순)
    @Transactional (readOnly = true)
    public List<AuditDto> findAll(){
 
    //  1. 감사 로그 전체 조회 (사용자 , 작업 유형까지 같이)     → 쿼리 1번
        List<AuditEntity> auditEntities = auditRepository.findAllWithMemberAndAction();
 
    //  2. 엔티티 → DTO 로 변환
        return auditEntities.stream().map((auditEntity)->{
 
            return AuditDto.from(auditEntity);
 
        }).toList();
    }
 
}