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

    // Entity -> DTO (로그인 / 내 정보 조회 응답용)
    // - 비밀번호(userPassword)는 응답에 실리면 안 되므로 넣지 않음 (null)
    // - signupEntity , roleEntity 는 LAZY 로딩이라 JPA 가짜 객체(프록시)가 들어있음
    //   → 그대로 JSON 으로 보내면 오류가 날 수 있어서 필요한 값만 새 객체에 옮겨 담음
    // - 프록시 값을 꺼내야 하므로 @Transactional 서비스 안에서 호출할 것
    public static MemberDto from(MemberEntity memberEntity) {

        SignupEntity signup = memberEntity.getSignupEntity();
        RoleEntity role = memberEntity.getRoleEntity();

        return MemberDto.builder()
                .memberId(memberEntity.getMemberId())
                .userEmail(memberEntity.getUserEmail())
                .companyName(memberEntity.getCompanyName())
                .managerName(memberEntity.getManagerName())
                .businessRegNo(memberEntity.getBusinessRegNo())
                .userPhone(memberEntity.getUserPhone())
                .companyAddress(memberEntity.getCompanyAddress())
                .status(memberEntity.getStatus())
                .signupEntity(SignupEntity.builder()
                        .signupId(signup.getSignupId())
                        .signupType(signup.getSignupType())
                        .build())
                .roleEntity(RoleEntity.builder()
                        .roleId(role.getRoleId())
                        .roleName(role.getRoleName())
                        .roleDescription(role.getRoleDescription())
                        .build())
                .build();
    }
}