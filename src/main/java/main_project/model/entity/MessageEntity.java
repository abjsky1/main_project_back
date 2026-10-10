package main_project.model.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
//  =====================================================================
@Entity
@Table(name = "message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)   // @CreatedDate 가 동작하도록 JPA 자동 기록 기능 연결 (BaseTime 에 붙어 있는 것과 같음)
public class MessageEntity {

    // 메시지 번호 (PK , 자동 증가) → 번호가 작을수록 먼저 보낸 메시지
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;

    // 어느 채팅방의 메시지인지 (FK → room 테이블의 room_id)
    //  - @ManyToOne : 메시지 여러 개(Many) → 채팅방 1개(One)
    //  - optional = false , nullable = false : 방 없는 메시지는 없음
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity roomEntity;

    // 보낸 사람 종류 : "MEMBER" / "ADMIN" / "SYSTEM" 중 하나
    //  - 3가지로 고정이고 늘어날 일이 없어서 role · action 같은 기초 테이블을 따로 만들지 않고 글자로 저장
    //  - 글자는 ChatService 의 private 상수로만 넣음 (오타 방지 — 다른 클래스는 ChatService 메소드를 부름)
    @Column(name = "sender_type", nullable = false, length = 10)
    private String senderType;

    // 보낸 사람 이름 복사본 (보낸 그 순간의 이름)
    //  - ADMIN  : 답한 상담원 이름 → 상담원이 여러 명이어도 누가 어떤 안내를 했는지 남음 (증거 · 교육 자료)
    //  - MEMBER : 회원 이름
    //  - SYSTEM : "시스템" (사람이 보낸 게 아니지만 빈칸 없이 채움 → 모든 메시지에 이름이 있어서 NOT NULL)
    //  - FK(회원 번호)가 아니라 이름 글자를 복사하는 이유 :
    //      상담원의 이름 · 권한이 나중에 바뀌어도 기록은 그대로 , 회원이 지워져도 FK 문제 없음
    //  ※ 회원 화면에는 상담원 이름 대신 "고객센터" 로만 보여줌 (프론트에서 처리)
    @Column(name = "sender_name", nullable = false, length = 50)
    private String senderName;

    // 메시지 내용 (최대 1000자 — 넘으면 ChatService 에서 저장하지 않음)
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    // 보낸 시각 (저장될 때 JPA 가 자동으로 현재 시각을 넣어 줌)
    //  - updatable = false : 한 번 저장된 뒤에는 UPDATE 문에 포함되지 않음 → 보낸 시각이 바뀔 일 없음
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
