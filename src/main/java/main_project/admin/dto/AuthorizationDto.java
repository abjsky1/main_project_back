package main_project.admin.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.member.entity.MemberEntity;

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

//  회원 엔티티 + 최근 로그인 시각 → 화면으로 보낼 DTO
//  - 이름 · 이메일 : 엔티티로 읽을 때 EncryptedStringConverter 가 이미 복호화해 둔 평문
//  - 역할 이름 : MemberRepository.searchWithRole 이 JOIN FETCH 로 같이 가져와서 추가 조회 없음
//  - 최근 로그인 : member 테이블에 없는 값이라 AuditRepository.findLastLogins 로 따로 구해서 받음 (기록이 없으면 null)
    public static AuthorizationDto from(MemberEntity memberEntity, LocalDateTime lastLoginAt){
        return AuthorizationDto.builder()
            .memberId(memberEntity.getMemberId())
            .managerName(memberEntity.getManagerName())
            .userEmail(memberEntity.getUserEmail())
            .roleName(memberEntity.getRoleEntity().getRoleName())
            .status(memberEntity.getStatus())
            .lastLoginAt(lastLoginAt)
            .build();
    }


}
