package main_project.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import main_project.model.entity.MessageEntity;
import main_project.model.entity.SenderType;

//  고객센터 채팅 메시지 Repository
//  - 메소드 이름 규칙은 RoomRepository 위쪽 주석 참고
//  - 정렬은 보낸 시각(createdAt) 대신 메시지 번호(messageId)로 함
//    → 번호는 서버가 받은 순서대로 1씩 커지므로(Redis 번호표) 같은 순간에 보낸 메시지도 순서가 섞이지 않음
//  - 저장(INSERT)은 이 Repository 가 아니라 ChatService ⑬ 이 여러 개를 모아서 한 번에 함
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Integer> {

    // 한 방의 메시지 전체 (관리자 화면 — 지난 상담까지 모두)
    //  - RoomEntityRoomId : 엔티티의 roomEntity 칸 → 그 안의 roomId 칸
    //  SELECT * FROM message WHERE room_id = ? ORDER BY message_id ASC
    List<MessageEntity> findByRoomEntityRoomIdOrderByMessageIdAsc(Integer roomId);

    // 한 방의 "이번 상담" 메시지만 (회원 화면 — 다시 시작하면 새 창처럼)
    //  - from 에는 채팅방의 opened_at(마지막으로 켜진 시각)을 넣음
    //  - CreatedAtGreaterThanEqual : created_at >= ? (켜진 그 순간의 "상담이 시작되었습니다" 메시지도 포함)
    //  SELECT * FROM message WHERE room_id = ? AND created_at >= ? ORDER BY message_id ASC
    List<MessageEntity> findByRoomEntityRoomIdAndCreatedAtGreaterThanEqualOrderByMessageIdAsc(Integer roomId, LocalDateTime from);

    // 한 방의 마지막 메시지 1개 (관리자 채팅방 목록의 미리보기 · "n분 전" 표시)
    //  - findFirst + OrderByMessageIdDesc : 번호가 가장 큰(= 가장 최근) 메시지 1개
    //  SELECT * FROM message WHERE room_id = ? ORDER BY message_id DESC LIMIT 1
    Optional<MessageEntity> findFirstByRoomEntityRoomIdOrderByMessageIdDesc(Integer roomId);

    // 안 읽은 메시지 수 세기
    //  - senderType 에 "상대방" 종류를 넣음 (관리자가 볼 때는 SenderType.MEMBER , 회원이 볼 때는 SenderType.ADMIN)
    //    → SYSTEM 메시지는 어느 쪽에서도 세지 않음
    //    → enum 이라 SenderType 의 3가지 말고는 넣을 수 없음 (JPA 가 SQL 에는 "MEMBER" 같은 글자로 바꿔서 넣음)
    //  - readAt 에는 "내가 마지막으로 읽은 시각" 을 넣음 (CreatedAtAfter : created_at > ?)
    //  SELECT COUNT(*) FROM message WHERE room_id = ? AND sender_type = ? AND created_at > ?
    long countByRoomEntityRoomIdAndSenderTypeAndCreatedAtAfter(Integer roomId, SenderType senderType, LocalDateTime readAt);

    // 지금까지 저장된 가장 큰 메시지 번호 (메시지가 없으면 0)
    //  - 서버가 켜질 때 Redis 번호표(chat:msg:seq)를 이 번호부터 이어서 시작하려고 (ChatService ⑫)
    //  - COALESCE( 값 , 0 ) : 값이 NULL(메시지가 하나도 없음)이면 0
    //  SELECT COALESCE(MAX(message_id), 0) FROM message
    @Query("SELECT COALESCE(MAX(m.messageId), 0) FROM MessageEntity m")
    Integer findMaxMessageId();

}
