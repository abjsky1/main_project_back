package main_project.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  =====================================================================
//  고객센터 채팅 메시지 (message 테이블)
//
//  - 채팅방 1개에 메시지 여러 개 (room 1 : N message)
//  - 보낸 사람 종류 (sender_type)
//      MEMBER : 회원이 보낸 메시지
//      ADMIN  : 고객센터(상담원)가 보낸 메시지 (상담 시작 인사 포함)
//      SYSTEM : "상담이 시작되었습니다" 같은 안내 → 상담을 켜고 끈 이력이 대화 안에 남음
//               (springweb day062 의 ENTER / QUIT 메시지를 고객센터용으로 바꾼 것)
//
//  ※ 감사 로그(AuditEntity)처럼 "기록" 이라서 한 번 저장하면 고치지 않음
//    → BaseTime 대신 created_at 만 두고 updatable = false
//
//  ※ 저장은 JPA(save)가 아니라 ChatService ⑬ 이 여러 개를 모아서 INSERT 1번으로 함 (Redis Stream → DB)
//    → 이 엔티티는 "읽기" 에만 씀 (메시지 목록 · 마지막 메시지 · 안 읽음 수)
//    → 그래서 번호 · 시각을 DB 나 JPA 가 정하지 않고 , 메시지를 받은 순간 ChatService 가 정해서 넣음
//  =====================================================================
@Entity
@Table(name = "message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {

    // 메시지 번호 (PK) → 번호가 작을수록 먼저 보낸 메시지
    //  - 자동 증가(@GeneratedValue)가 아님 : Redis 번호표(INCR chat:msg:seq)가 메시지를 받는 순간 1 , 2 , 3 ... 을 줌
    //    (DB 에 넣기 몇 초 전부터 화면에 번호가 필요해서 — 프론트가 이 번호로 중복을 거르고 순서를 정함)
    //  - PK 라서 같은 번호는 두 번 들어가지 않음 → 옮기다 서버가 꺼져서 다시 옮겨도 중복 저장 없음 (INSERT IGNORE)
    @Id
    @Column(name = "message_id")
    private Integer messageId;

    // 어느 채팅방의 메시지인지 (FK → room 테이블의 room_id)
    //  - @ManyToOne : 메시지 여러 개(Many) → 채팅방 1개(One)
    //  - optional = false , nullable = false : 방 없는 메시지는 없음
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity roomEntity;

    // 보낸 사람 종류 : SenderType.MEMBER / ADMIN / SYSTEM 중 하나 (SenderType.java — enum)
    //  - 3가지로 고정이고 늘어날 일이 없어서 role · action 같은 기초 테이블을 따로 만들지 않고 글자로 저장
    //  - 자바에서는 enum 이라 이 3가지 말고는 넣을 수 없음 (오타는 컴파일할 때 바로 빨간 줄)
    //  - @Enumerated(EnumType.STRING) : DB 에 enum 의 "이름 글자" 를 저장 ("MEMBER" / "ADMIN" / "SYSTEM")
    //      ⚠️ STRING 을 꼭 적어야 함 — 안 적으면 기본값 ORDINAL 이라 선언 순서 번호(0 , 1 , 2)가 저장됨
    //         → 나중에 enum 순서를 바꾸거나 중간에 값을 끼워 넣으면 이미 저장된 번호의 뜻이 통째로 바뀜
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 10)
    private SenderType senderType;

    // 보낸 사람 이름 복사본 (보낸 그 순간의 이름)
    //  - ADMIN  : 답한 상담원 이름 → 상담원이 여러 명이어도 누가 어떤 안내를 했는지 남음 (증거 · 교육 자료)
    //  - MEMBER : 회원 이름
    //  - SYSTEM : "시스템" (사람이 보낸 게 아니지만 빈칸 없이 채움 → 모든 메시지에 이름이 있어서 NOT NULL)
    //  - FK(회원 번호)가 아니라 이름 글자를 복사하는 이유 :
    //      상담원의 이름 · 권한이 나중에 바뀌어도 기록은 그대로 , 회원이 지워져도 FK 문제 없음
    //  ※ 회원 화면에는 상담원 이름 대신 "고객센터" 로만 보여줌
    //    (ChatService 가 회원에게 보낼 때 바꿔서 보냄 → 상담원 이름이 회원 브라우저에 아예 전달되지 않음)
    @Column(name = "sender_name", nullable = false, length = 50)
    private String senderName;

    // 메시지 내용 (최대 1000자 — 넘으면 ChatService 에서 저장하지 않음)
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    // 보낸 시각 (서버가 메시지를 받은 순간 — ChatService 가 정함)
    //  - DB 에 들어간 시각이 아님 (몇 초 늦게 들어가도 보낸 시각은 그대로 → 이번 상담 · 안 읽음 계산이 틀어지지 않음)
    //  - updatable = false : 한 번 저장된 뒤에는 UPDATE 문에 포함되지 않음 → 보낸 시각이 바뀔 일 없음
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
