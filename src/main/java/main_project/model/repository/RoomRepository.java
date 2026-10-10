package main_project.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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

}
