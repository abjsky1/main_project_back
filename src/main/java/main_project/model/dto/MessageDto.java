package main_project.model.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.MessageEntity;

//  =====================================================================
//  채팅 메시지 1개 (서버 → 프론트)
//
//  - 쓰이는 곳 2군데 (모양이 같아서 하나로 같이 씀)
//      1. REST 응답    : 채팅방을 열 때 지난 대화 목록 (GET /api/chat/rooms/{roomId}/messages 등)
//      2. 웹소켓 전송  : 새 메시지가 오면 구독 주소(/sub/chat/room/{roomId} , /sub/chat/admin)로 보냄
//  - springweb day062 의 MessageDto 와 비교
//      type(TALK/ENTER/QUIT) → senderType(MEMBER/ADMIN/SYSTEM)
//      sender(화면이 보낸 닉네임) → senderName(서버가 저장한 이름 — 화면이 보낸 값은 믿지 않음)
//      date(화면이 만든 시각) → createdAt(DB 에 저장된 시각)
//  =====================================================================
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MessageDto {

//  메시지 번호 (프론트에서 말풍선 목록의 key 로 사용)
    private Integer messageId;

//  어느 방의 메시지인지 (관리자 화면은 모든 방의 메시지를 한 주소로 받아서 , 이 번호로 방을 구분)
    private Integer roomId;

//  보낸 사람 종류 : MEMBER / ADMIN / SYSTEM (말풍선 위치 · 모양을 정함)
    private String senderType;

//  보낸 사람 이름
//  - 관리자에게 보낼 때 : 저장된 이름 그대로 (ADMIN 이면 답한 상담원 이름)
//  - 회원에게 보낼 때   : ADMIN 이면 "고객센터" 로 바꿔서 보냄 (ChatService 가 처리)
//                         → 상담원 이름이 회원 브라우저에 아예 전달되지 않음
    private String senderName;

//  메시지 내용
    private String content;

//  보낸 시각
//  - "2026-10-10T14:05:30" 모양으로 보냄 (가운데 T 가 있는 표준 모양)
//  - AuditDto 처럼 "2026-10-10 14:05:30" (빈칸) 으로 보내지 않는 이유 :
//    채팅 화면은 시각을 계산("오늘이면 14:05")해야 해서 JS 의 new Date( ) 로 읽어야 하는데 ,
//    빈칸 모양은 브라우저에 따라 못 읽는 경우가 있고 T 모양은 모든 브라우저가 읽을 수 있음
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

//  이 메시지를 보낼 때의 채팅방 상태 (true = 상담 중 , false = 종료)
//  - 웹소켓으로 "상담이 종료되었습니다" 가 오면 , 회원 화면이 이 값을 보고 바로 입력창을 잠금
    private Boolean roomStatus;


//  엔티티 → DTO (저장된 값을 그대로 옮김)
//  회원에게 보낼 때 상담원 이름을 "고객센터" 로 바꾸는 건 ChatService 가 이 값을 받은 뒤에 처리
//  (보낸 사람 종류 글자 "ADMIN" 은 ChatService 의 private 상수라서 , 판단도 ChatService 에서)
    public static MessageDto from(MessageEntity messageEntity){
        return MessageDto.builder()
            .messageId(messageEntity.getMessageId())
            .roomId(messageEntity.getRoomEntity().getRoomId())       // LAZY 라도 번호는 추가 조회 없이 꺼낼 수 있음
            .senderType(messageEntity.getSenderType())
            .senderName(messageEntity.getSenderName())
            .content(messageEntity.getContent())
            .createdAt(messageEntity.getCreatedAt())
            .roomStatus(messageEntity.getRoomEntity().getStatus())   // 방의 지금 상태 (같은 방 메시지들은 방을 한 번만 조회)
            .build();
    }

}
