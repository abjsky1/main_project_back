package main_project.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  =====================================================================
//  고객센터 채팅방 (room 테이블)
//
//  - 회원 1명당 채팅방 1개 , 대화 상대는 항상 "고객센터(관리자 전체)"
//  - status 가 상담 on/off 스위치
//      켜기(on)  : 회원이 [상담 시작]                         → status = true  , opened_at = 지금
//      끄기(off) : 상담원 또는 회원이 [상담 종료] , 회원 탈퇴  → status = false , closed_at = 지금
//  - 켜고 끈 "이력" 은 여기가 아니라 message 테이블에 SYSTEM 메시지로 남음
//    (여기에는 "마지막으로" 켜고 끈 시각만 있음)
//
//  ※ 채팅방을 지우는 기능은 일부러 만들지 않음 → 대화 기록은 감사 로그처럼 계속 보관
//    (회원이 탈퇴해도 방은 남고 member_id 만 NULL 로 비움 — MypageService 탈퇴 처리)
//  =====================================================================
@Entity
@Table(name = "room")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomEntity extends BaseTime {   // BaseTime 상속 → created_at(방을 처음 만든 시각) , updated_at(아무 칸이나 마지막으로 바뀐 시각) 자동 기록

    // 방 번호 (PK , 자동 증가) — 웹소켓 구독 주소 /sub/chat/room/{roomId} 에도 들어감
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId;

    // 이 방의 회원 (FK → member 테이블의 member_id)
    //  - @OneToOne : 회원 1명 ↔ 방 1개 (InterestEntity 는 회원 1명에 관심 국가 여러 개라서 @ManyToOne)
    //  - unique = true : 같은 member_id 가 두 번 들어갈 수 없음 → DB 가 "1인 1방" 을 지켜 줌
    //  - nullable = true , optional = true : 비어 있어도(NULL) 됨 → 탈퇴한 회원의 방은 NULL
    //      * FK 칸은 "member 테이블에 있는 회원 번호" 또는 "NULL" 만 넣을 수 있음
    //        회원을 지우기 전에 NULL 로 비우지 않으면 DB 가 회원 삭제를 거부함 (감사 로그를 옮기는 이유와 같음)
    //      * MySQL 은 UNIQUE 칸에 NULL 이 여러 개 있어도 괜찮음 → 탈퇴 회원 방이 여러 개여도 충돌 없음
    //  - fetch = LAZY : 방을 조회할 때 회원 정보는 바로 가져오지 않고 , 실제로 꺼내 쓸 때 가져옴 (불필요한 조회 방지)
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "member_id", nullable = true, unique = true)
    private MemberEntity memberEntity;

    // ---------- 회원 정보 복사본 ----------
    // 상담을 시작할 때마다 그때의 회원 정보로 다시 복사 (ChatService 의 상담 시작)
    //  - 탈퇴해서 member_id 가 NULL 이 돼도 "누구와 나눈 대화인지" 남음 → 증거 · 교육 자료
    //  - 관리자 채팅방 목록을 만들 때 member 테이블과 합치지(join) 않아도 됨
    //  - 길이는 member 테이블의 같은 칸과 똑같이 맞춤

    // 담당자 이름 (member.manager_name)
    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName;

    // 기업명 (member.company_name)
    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    // 회원 유형 (signup.signup_type , 예: 수출입기업 / 물류운송업체)
    @Column(name = "company_type", nullable = false, length = 20)
    private String companyType;

    // ---------- 상담 on/off 스위치 ----------

    // true = 상담 중(on) , false = 종료(off)
    //  - @Builder.Default : builder 로 방을 만들 때 값을 안 넣으면 true 로 시작
    //    (방은 회원이 [상담 시작] 을 누를 때 처음 만들어지므로 , 만들자마자 켜진 상태)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private Boolean status = true;

    // 마지막으로 켜진(상담 시작) 시각
    //  - 관리자 채팅방 목록 정렬 : 상담 중인 방은 이 시각이 오래된 순 (먼저 온 상담 먼저 = 선입선출)
    //  - 회원 화면 거르기 : 회원에게는 이 시각 이후의 메시지만 보여줌 (다시 시작하면 새 창처럼)
    //  ※ updated_at 은 읽음 처리처럼 다른 칸만 바뀌어도 갱신되므로 이 용도로 쓸 수 없음 → 칸을 따로 둠
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    // 마지막으로 꺼진(상담 종료) 시각 , 상담 중이면 NULL 일 수 있음
    //  - 관리자 채팅방 목록 정렬 : 종료된 방은 이 시각이 최근 순
    @Column(name = "closed_at", nullable = true)
    private LocalDateTime closedAt;

    // ---------- 읽음 표시 ----------
    // 안 읽은 메시지 수 = 상대가 보낸 메시지 중 "내가 마지막으로 읽은 시각" 보다 늦게 온 것 (SYSTEM 메시지는 빼고 셈)
    // 숫자를 따로 저장하지 않고 , 필요할 때 MessageRepository 로 세어서 계산

    // 회원이 마지막으로 읽은 시각
    @Column(name = "member_read_at", nullable = true)
    private LocalDateTime memberReadAt;

    // 고객센터(관리자)가 마지막으로 읽은 시각 — 관리자 중 누구라도 읽으면 갱신 (관리자 전체가 같은 고객센터)
    @Column(name = "admin_read_at", nullable = true)
    private LocalDateTime adminReadAt;

}
