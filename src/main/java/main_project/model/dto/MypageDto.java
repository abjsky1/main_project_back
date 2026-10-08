package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  마이페이지 "내 정보 수정" 요청 (이름 · 주소만 수정 가능)
//  어떤 회원인지는 주소(/api/mypage/{memberId})로 받음
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MypageDto {

//  담당자 이름 (member.manager_name)
    private String managerName;

//  회사 주소 (member.company_address)
    private String companyAddress;

}
