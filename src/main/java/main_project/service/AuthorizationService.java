// package main_project.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationDto;
import main_project.model.entity.AuditEntity;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.AuditRepository;
import main_project.model.repository.MemeberRepository;

// @Service 
// @RequiredArgsConstructor 
// @Transactional 
// public class AuthorizationService {

//     private final MemeberRepository memeberRepository;

//     private final AuditRepository auditRepository;

//  사용자 권한 관리 사용자 목록 조회
    public List<AuthorizationDto> AuthorizationFindAll(){

        // member 엔티티 전체 불러오기
        List memberEntities = memeberRepository.findAll();

        // Dto 로 변환해야 하니까 최종 반환할 Dto 리스트 생성
        List authorizationDtos = new ArrayList<>();

        // 전체 불러온 member 엔티티에서 하나씩 꺼내기
        memberEntities.forEach((memberEntity) -> {
            
            // 해당 회원의 최근 로그인(성공) 기록 찾기
            Optional recentLoginLog = auditRepository.findTopByMemberEntityAndLoginSuccessOrderByCreatedAtDesc(memberEntity);
            
            // 로그인 기록이 있으면 그 시간, 없으면 null을 반환
            LocalDateTime lastLoginTime = null;
            if(recentLoginLog.isPresent()){
                lastLoginTime = recentLoginLog.get().getCreatedAt();
            }

            // 꺼낸 Entity와 조회한 마지막 로그인 시간을 넘겨 Dto로 변환
            AuthorizationDto authorizationDto = AuthorizationDto.from(memberEntity, lastLoginTime);

            // 변환한 Dto를 리스트에 집어넣기
            authorizationDtos.add(authorizationDto);
        });

        return authorizationDtos;

    }
}











}
