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
}