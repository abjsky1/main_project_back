package main_project.admin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  사용자 권한 관리 화면에 필요한 데이터 한 묶음 (사용자 수 + 이번 페이지 사용자 목록 + 페이지 숫자)
//  예) { "allUser": 208, "activate": 205, "deactivate": 3,
//        "members": [ ...50명... ], "page": 0, "size": 50, "totalPages": 5, "totalCount": 208 }
//  페이지 숫자 4칸은 감사 로그 AuditPageDto 와 같은 모양
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

//  사용자 목록 (표에 들어갈 행들 — 전체가 아니라 이번 페이지 , 최대 50명)
    private List<AuthorizationDto> members;

//  지금 페이지 번호 — 0 부터 (0 = 1페이지) , 화면에서 보여 줄 때 +1
    private Integer page;

//  한 페이지에 몇 명씩 (50)
    private Integer size;

//  전체 페이지 수 (필터에 맞는 사람이 없으면 0)
    private Integer totalPages;

//  필터에 맞는 전체 인원 (화면의 "사용자 목록 N명") — 위의 allUser 는 필터와 상관없는 전체 인원
    private Long totalCount;


//  전체 사용자수, 활성 사용자수, 비활성 사용자수 는 Service 에서 Count 를 해서 대입할 것


}