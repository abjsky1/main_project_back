package main_project.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  사용자 권한 관리 화면에 필요한 데이터 한 묶음 (사용자 수 + 사용자 목록)
@AllArgsConstructor 
@NoArgsConstructor 
@Data 
@Builder 
public class AuthorizationCountDto {

//  전체 사용자수
    private Integer allUser;

//  활성 사용자수
    private Integer activate;

//  비활성 사용자수
    private Integer deactivate;

//  사용자 목록 (표에 들어갈 행들)
    private List<AuthorizationDto> members;

    
//  전체 사용자수, 활성 사용자수, 비활성 사용자수 는 Service 에서 Count 를 해서 대입할 것


}