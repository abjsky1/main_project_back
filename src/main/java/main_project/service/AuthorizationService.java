package main_project.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.AuthorizationCountDto;
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
    public AuthorizationCountDto findAll(){

    //  1. 회원 전체 조회 (역할까지 같이)                        → 쿼리 1번
        List<MemberEntity> memberEntities = memberRepository.findAllWithRole();

    //  2. 회원별 최근 로그인 시간 조회                          → 쿼리 1번
        Map<Integer, LocalDateTime> lastLoginMap = new HashMap<>();

        auditRepository.findLastLoginList().forEach((lastLogin)->{
            lastLoginMap.put(lastLogin.getMemberId(), lastLogin.getLastLoginAt());
        });

    //  3. 회원 + 최근 로그인 시간 → 표에 들어갈 DTO 목록
        List<AuthorizationDto> authorizationDtos = memberEntities.stream().map((memberEntity)->{

            LocalDateTime lastLoginAt = lastLoginMap.get(memberEntity.getMemberId());   // 로그인 기록이 없으면 null

            return AuthorizationDto.from(memberEntity, lastLoginAt);

        }).toList();

    //  4. 사용자 수 세기 (1번에서 가져온 목록으로 세니까 쿼리 추가 없음 + 표와 숫자가 항상 일치)
        int allUser = memberEntities.size();

        int activate = (int) memberEntities.stream()
                .filter((memberEntity) -> memberEntity.getStatus())
                .count();

        int deactivate = allUser - activate;

    //  5. 사용자 수 + 목록 → 응답 DTO 하나로 묶기
        AuthorizationCountDto result = AuthorizationCountDto.builder()
            .allUser(allUser)
            .activate(activate)
            .deactivate(deactivate)
            .members(authorizationDtos)
            .build();

        return result;
    }


}
