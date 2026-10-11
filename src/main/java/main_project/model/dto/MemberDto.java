package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.MemberEntity;
import main_project.model.entity.RoleEntity;
import main_project.model.entity.SignupEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {

    private String memberId;

    private String userEmail;

    private String userPassword;

    private String companyName;

    private String managerName;

    private String businessRegNo;

    private String userPhone;

    private String companyAddress;

    private Boolean status;

    private SignupEntity signupEntity;

    private RoleEntity roleEntity;


    // DTO -> Entity
    public MemberEntity toEntity() {

        return MemberEntity.builder()
                .memberId(memberId)
                .signupEntity(signupEntity)
                .userEmail(userEmail)
                .userPassword(userPassword)
                .companyName(companyName)
                .managerName(managerName)
                .businessRegNo(businessRegNo)
                .userPhone(userPhone)
                .companyAddress(companyAddress)
                .roleEntity(roleEntity)
                .status(status)
                .build();
    }

    // Entity -> DTO
    public static MemberDto from(MemberEntity memberEntity) {

        // 회원에게 연결된 권한·가입 유형 조회
    RoleEntity role = memberEntity.getRoleEntity();
    SignupEntity signup = memberEntity.getSignupEntity();

    // 응답에 필요한 권한 정보만 복사
    RoleEntity roleInfo = role == null ? null
            : RoleEntity.builder()
                    .roleId(role.getRoleId())
                    .roleName(role.getRoleName())
                    .build();

    // 응답에 필요한 가입 유형 정보만 복사
    SignupEntity signupInfo = signup == null ? null
            : SignupEntity.builder()
                    .signupId(signup.getSignupId())
                    .signupType(signup.getSignupType())
                    .build();

    return MemberDto.builder()
            .memberId(memberEntity.getMemberId())
            .userEmail(memberEntity.getUserEmail())
            .companyName(memberEntity.getCompanyName())
            .managerName(memberEntity.getManagerName())
            .businessRegNo(memberEntity.getBusinessRegNo())
            .userPhone(memberEntity.getUserPhone())
            .companyAddress(memberEntity.getCompanyAddress())
            .status(memberEntity.getStatus())
            .roleEntity(roleInfo)       // 추가
            .signupEntity(signupInfo)   // 추가
            .build();
}
}