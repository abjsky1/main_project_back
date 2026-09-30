package main_project.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.MemberEntity;
import main_project.model.entity.RoleEntity;
import main_project.model.entity.SignupEntity;

@AllArgsConstructor 
@NoArgsConstructor 
@Data 
@Builder 
public class AuthorizationDto {

//  회원 번호
    private Integer memberId;

//  회원 이메일
    private String userEmail;

//  회원 권환
    private RoleEntity roleEntity;

//  회원 상태
    @Builder.Default
    private Boolean status = true;

//  최근 로그인 (감사로그에서 조회된 시간)
    private LocalDateTime createdAt;

//  [Entity -> DTO 변환] (마지막 로그인 시간을 추가 파라미터로 받음)
    public static AuthorizationDto from(MemberEntity memberEntity, LocalDateTime lastLoginTime){
        return AuthorizationDto.builder()
            .memberId(memberEntity.getMemberId())
            .userEmail(memberEntity.getUserEmail())
            .roleEntity(memberEntity.getRoleEntity())
            .status(memberEntity.getStatus())
            .lastLoginTime(lastLoginTime) // 여기서 세팅
            .build();
    }

//  [DTO -> Entity 변환] (조회용 DTO라 실제론 잘 안 쓰이지만 구조 유지를 위해 작성)
    public MemberEntity toEntity(){
        return MemberEntity.builder()
            .memberId(this.memberId)
            .userEmail(this.userEmail)
            .roleEntity(this.roleEntity)
            .status(this.status)
            .build();
    }

}
