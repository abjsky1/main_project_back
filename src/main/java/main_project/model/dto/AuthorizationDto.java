package main_project.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private Boolean status = true;

//  최근 로그인
    private LocalDateTime createdAt;

    

}
