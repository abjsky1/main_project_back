package main_project.model.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.MemberEntity;

@AllArgsConstructor 
@NoArgsConstructor 
@Data 
@Builder 
public class AuthorizationDto {

//  회원 번호
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

//  최근 로그인 시간은 MemberEntity 안에 없으니까 매개변수로 따로 받음
    public static AuthorizationDto from(MemberEntity memberEntity , LocalDateTime lastLoginAt){
        return AuthorizationDto.builder()
            .memberId(memberEntity.getMemberId())
            .managerName(memberEntity.getManagerName())
            .userEmail(memberEntity.getUserEmail())
            .roleName(memberEntity.getRoleEntity().getRoleName())   // ※ RoleEntity 실제 필드명으로 바꿔줘
            .status(memberEntity.getStatus())
            .lastLoginAt(lastLoginAt)
            .build();
    }


}
