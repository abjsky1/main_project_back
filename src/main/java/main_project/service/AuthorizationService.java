package main_project.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.AuditRepository;
import main_project.model.repository.MemberRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class AuthorizationService {

    private final MemberRepository memberRepository;

    private final AuditRepository auditRepository;

//  사용자 권한 관리 사용자 목록 조회
    @Transactional (readOnly = true)
    public List<AuthorizationDto> findAll(){

    //  1. 회원 전체 조회 (역할까지 같이)                        → 쿼리 1번
        List<MemberEntity> memberEntities = memberRepository.findAllWithRole();

    //  2. 회원별 최근 로그인 시간 조회                          → 쿼리 1번
    //     { 회원번호 : 최근로그인시간 } 형태의 Map 으로 바꿔두면 회원번호로 바로 꺼낼 수 있음
        Map<Integer, LocalDateTime> lastLoginMap = new HashMap<>();

        auditRepository.findLastLoginList().forEach((lastLogin)->{
            lastLoginMap.put(lastLogin.getMemberId(), lastLogin.getLastLoginAt());
        });

    //  3. 회원 + 최근 로그인 시간 → DTO 로 변환
        return memberEntities.stream().map((memberEntity)->{

            LocalDateTime lastLoginAt = lastLoginMap.get(memberEntity.getMemberId());   // 로그인 기록이 없으면 null

            return AuthorizationDto.from(memberEntity, lastLoginAt);

        }).toList();
    }


}
