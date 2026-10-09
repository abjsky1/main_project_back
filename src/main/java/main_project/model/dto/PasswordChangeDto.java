package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//  비밀번호 변경 요청 (PUT /api/mypage/password)
//  어떤 회원인지는 쿠키(AccessToken)로 정하고 , body 로는 비밀번호 2개만 받음
//  (둘 다 입력한 그대로의 평문 — 서버는 BCrypt 로 비교 · 암호화만 하고 평문은 어디에도 저장하지 않음)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeDto {

//  지금 쓰고 있는 비밀번호 (본인 확인용)
    private String currentPassword;

//  새로 바꿀 비밀번호
    private String newPassword;

}
