package main_project.model.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.repository.MemberRepository;

@AllArgsConstructor 
@NoArgsConstructor 
@Data 
@Builder 
public class AuthorizationDto {

//  회원 아이디 (UUID)
    private String memberId;

//  회원 이름
    private String managerName;

//  회원 이메일
    private String userEmail;

//  회원 권환
    private String roleName;

//  회원 상태
    @Builder.Default
    private Boolean status = true;

//  최근 로그인 (감사로그에서 조회된 시간 , 화면 모양 그대로 "2025-12-31 09:14" 로 내려감)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime lastLoginAt;



//  조회 전용 DTO 라서 toEntity() 는 만들지 않음.
//  (비밀번호, 사업자번호 등 필수값이 없어서 MemberEntity 를 만들 수도 없음)

//  사용자 목록 쿼리 결과 한 줄(MemberRow) → 화면으로 보낼 DTO
//  역할 이름 · 최근 로그인까지 쿼리가 한 번에 만들어 주므로 그대로 옮겨 담기만 함
    public static AuthorizationDto from(MemberRepository.MemberRow memberRow){
        return AuthorizationDto.builder()
            .memberId(memberRow.getMemberId())
            .managerName(memberRow.getManagerName())
            .userEmail(memberRow.getUserEmail())
            .roleName(memberRow.getRoleName())
            .status(memberRow.getStatus())
            .lastLoginAt(memberRow.getLastLoginAt())   // 로그인 기록이 없으면 null
            .build();
    }


}
