package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//  회원 탈퇴 요청 (POST /api/mypage/withdraw)
//  어떤 회원인지는 쿠키(AccessToken)로 정하고 , body 로는 비밀번호만 받음
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawDto {

//  탈퇴 확인용 비밀번호 (입력한 그대로의 평문 — 서버는 BCrypt 로 비교만 하고 어디에도 저장하지 않음)
    private String userPassword;

}
