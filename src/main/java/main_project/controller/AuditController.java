package main_project.controller;
 
import java.util.List;
 
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuditDto;
import main_project.service.AuditService;
 
@RestController
@RequiredArgsConstructor
@RequestMapping ("/doodoo/audit")      // ※ 주소는 팀 규칙에 맞게 바꿔줘
public class AuditController {
 
    private final AuditService auditService;
 
 
//  감사 로그 목록 조회
    @GetMapping ("")
    public List<AuditDto> findAll(){
 
        return auditService.findAll();
    }
 
}