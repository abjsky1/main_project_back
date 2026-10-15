package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//  =====================================================================
//  메시지 보내기 요청 (프론트 → 서버 , 웹소켓 발행 주소 /pub/chat/message 의 body)
//
//  - 받는 값은 2개뿐 : 어느 방에 , 무슨 내용을
//  - 누가 보냈는지(senderType , senderName) 와 보낸 시각은 받지 않음
//      → 서버가 웹소켓 연결 때 쿠키로 확인해 둔 회원 정보로 정함 (화면이 보낸 값은 믿지 않음)
//      → springweb day062 의 MessageDto 는 화면이 sender(닉네임)를 보내서 , 다른 사람 이름으로도 보낼 수 있었음
//  - PasswordChangeDto 처럼 요청 body 를 담는 용도라서 builder 없이 기본 생성자 + setter 만 있으면 됨
//    (JSON 글자 → 이 객체로 바꿀 때 Spring 이 빈 객체를 만든 뒤 setter 로 값을 넣음)
//  =====================================================================
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendDto {

//  보낼 채팅방 번호 (서버가 "이 방의 주인 또는 관리자" 인지 확인한 뒤에만 저장)
    private Integer roomId;

//  메시지 내용 (서버에서 앞뒤 빈칸을 지우고 , 비었거나 1000자를 넘으면 저장하지 않음)
    private String content;

}
