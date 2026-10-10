package main_project.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.RoomEntity;

//  =====================================================================
//  채팅방 1개 (서버 → 프론트)
//
//  - 쓰이는 곳
//      관리자 채팅방 목록 (GET /api/chat/rooms)         → 목록 한 줄 (messages 는 비워서 보냄 — 목록이 무거워지지 않게)
//      회원 내 채팅방   (GET /api/chat/my-room 등)      → 방 정보 + 이번 상담 메시지(messages)
//  - lastMessage , unreadCount , messages 는 엔티티에 없는 값이라 ChatService 가 계산해서 넣음
//    (AuthorizationCountDto 의 사용자 수를 Service 가 세서 넣는 것과 같은 방식)
//  - 회원 번호(member_id)는 보내지 않음 → 화면에 필요 없고 , 탈퇴 여부는 withdrawn 으로 알려 줌
//  =====================================================================
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RoomDto {

//  방 번호 (프론트에서 목록의 key , 웹소켓 구독 주소 /sub/chat/room/{roomId} 에 사용)
    private Integer roomId;

//  회원 정보 복사본 (상담을 시작할 때 복사해 둔 값 — 탈퇴한 회원도 그대로 보임)
    private String managerName;
    private String companyName;
    private String companyType;

//  탈퇴한 회원의 방인지 (true 면 화면에 "탈퇴 회원" 표시)
//  - 엔티티의 member_id 가 NULL 이면 탈퇴
    private Boolean withdrawn;

//  상담 on/off (true = 상담 중 , false = 종료)
    private Boolean status;

//  마지막으로 켜진 시각 / 꺼진 시각 (MessageDto 의 createdAt 과 같은 모양)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime openedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime closedAt;

//  ---------- ChatService 가 계산해서 넣는 값 ----------

//  가장 최근 메시지 1개 (관리자 목록의 미리보기 · "n분 전" 표시 , 메시지가 없으면 null)
    private MessageDto lastMessage;

//  보는 사람 입장에서 안 읽은 메시지 수 (관리자 목록 → 관리자 기준 , 내 채팅방 → 회원 기준)
//  - Long : Repository 의 countBy 가 long 을 돌려줘서 그대로 받음
    private Long unreadCount;

//  메시지 목록 (회원 내 채팅방 응답에서만 채움 , 관리자 목록에서는 null)
//  - 회원에게는 이번 상담(opened_at 이후) 메시지만 들어감
    private List<MessageDto> messages;


//  엔티티 → DTO (방 자체의 값만 옮김 , 나머지는 ChatService 가 setter 로 채움)
//  - @Data 가 setLastMessage( ) 같은 setter 를 만들어 줌
    public static RoomDto from(RoomEntity roomEntity){
        return RoomDto.builder()
            .roomId(roomEntity.getRoomId())
            .managerName(roomEntity.getManagerName())
            .companyName(roomEntity.getCompanyName())
            .companyType(roomEntity.getCompanyType())
            .withdrawn(roomEntity.getMemberEntity() == null)   // LAZY 라도 "비어 있는지" 는 추가 조회 없이 알 수 있음
            .status(roomEntity.getStatus())
            .openedAt(roomEntity.getOpenedAt())
            .closedAt(roomEntity.getClosedAt())
            .build();
    }

}
