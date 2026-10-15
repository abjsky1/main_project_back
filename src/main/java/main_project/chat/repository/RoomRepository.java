package main_project.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import main_project.model.entity.RoomEntity;

//  고객센터 채팅방 Repository
//  - JpaRepository<엔티티 , PK 타입> 를 상속하면 save / findById / findAll 같은 기본 메소드가 자동으로 생김
//  - 아래 메소드들은 "이름 규칙" 만 지켜서 선언하면 JPA 가 이름을 읽고 SQL 을 대신 만들어 줌 (구현 코드 없음)
//      findBy  + 칸 이름 + 조건   →  SELECT ... WHERE ...
//      OrderBy + 칸 이름 + Asc/Desc →  ORDER BY ... ASC(오래된 순) / DESC(최근 순)
//      칸 이름은 DB 칸 이름(snake_case)이 아니라 엔티티 변수 이름(camelCase)을 씀  예) openedAt
@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Integer> {

    // 회원의 채팅방 찾기 (회원 1명당 방 1개라서 결과도 0개 또는 1개)
    //  - MemberEntityMemberId : 엔티티의 memberEntity 칸 → 그 안의 memberId 칸 (점 대신 이름을 이어 붙임)
    //  - Optional : "결과가 없을 수도 있음" 을 담는 상자 → .orElse(null) 로 꺼내면 없을 때 null
    //  SELECT * FROM room WHERE member_id = ?
    Optional<RoomEntity> findByMemberEntityMemberId(String memberId);

    // 관리자 채팅방 목록 ① 상담 중인 방 — 켜진 시각이 오래된 순 (먼저 온 상담이 위 = 선입선출)
    //  - StatusTrue : status 칸이 true 인 것만 (값을 넘기지 않아도 됨)
    //  SELECT * FROM room WHERE status = true ORDER BY opened_at ASC
    List<RoomEntity> findByStatusTrueOrderByOpenedAtAsc();

    // 관리자 채팅방 목록 ② 종료된 방 — 꺼진 시각이 최근 순 (방금 끝난 상담이 위)
    //  SELECT * FROM room WHERE status = false ORDER BY closed_at DESC
    List<RoomEntity> findByStatusFalseOrderByClosedAtDesc();

    // 읽은 시각만 바로 바꾸기 (ChatService ⑥ 읽음 처리 , ⑦ 메시지 보내기)
    //  - 왜 setter 를 쓰지 않는가 : setter 로 바꾸려면 방을 먼저 SELECT 해야 하고 ,
    //    JPA 가 만드는 UPDATE 는 바뀌지 않은 칸까지 모든 칸을 다시 씀 (콘솔의 update room set admin_read_at=? , closed_at=? ... )
    //    → 이 쿼리는 SELECT 없이 필요한 칸 2개만 UPDATE
    //  - @Query : 이름 규칙 대신 직접 적은 JPQL (테이블 · 칸 이름이 아니라 엔티티 · 변수 이름을 씀)
    //  - @Modifying : SELECT 가 아니라 값을 바꾸는 쿼리라는 표시 (없으면 실행할 때 오류)
    //  - updatedAt : 이렇게 직접 UPDATE 하면 BaseTime 의 자동 기록(@LastModifiedDate)이 동작하지 않아서 같이 넣음
    //  - 돌려주는 값 : 바뀐 줄 수 (없는 방이면 0)
    //  UPDATE room SET member_read_at = ? , updated_at = ? WHERE room_id = ?
    @Modifying
    @Query("UPDATE RoomEntity r SET r.memberReadAt = :readAt, r.updatedAt = :readAt WHERE r.roomId = :roomId")
    int updateMemberReadAt(@Param("roomId") Integer roomId, @Param("readAt") LocalDateTime readAt);

    //  UPDATE room SET admin_read_at = ? , updated_at = ? WHERE room_id = ?
    @Modifying
    @Query("UPDATE RoomEntity r SET r.adminReadAt = :readAt, r.updatedAt = :readAt WHERE r.roomId = :roomId")
    int updateAdminReadAt(@Param("roomId") Integer roomId, @Param("readAt") LocalDateTime readAt);

}
