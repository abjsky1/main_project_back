package main_project.model.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import main_project.model.entity.MessageEntity;

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

    // ※ 예전에 있던 "방의 마지막 메시지" · "안 읽은 메시지 수 세기" 는 삭제함
    //   → Redis(chat:last:{방번호} , chat:unread:...)가 바로 들고 있어서 DB 를 셀 필요가 없음 (ChatService ⑨ ⑫)

    // 지금까지 저장된 가장 큰 메시지 번호 (메시지가 없으면 0)
    //  - 서버가 켜질 때 Redis 번호표(chat:msg:seq)를 이 번호부터 이어서 시작하려고 (ChatService ⑫)
    //  - COALESCE( 값 , 0 ) : 값이 NULL(메시지가 하나도 없음)이면 0
    //  SELECT COALESCE(MAX(message_id), 0) FROM message
    @Query("SELECT COALESCE(MAX(m.messageId), 0) FROM MessageEntity m")
    Integer findMaxMessageId();

}
