package main_project.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MessageDto;
import main_project.model.dto.RoomDto;
import main_project.model.entity.MemberEntity;
import main_project.model.entity.MessageEntity;
import main_project.model.entity.RoomEntity;
import main_project.model.repository.MemberRepository;
import main_project.model.repository.MessageRepository;
import main_project.model.repository.RoomRepository;

//  =====================================================================
//  고객센터 채팅 서비스
//
//  - 누가 요청했는지는 컨트롤러가 쿠키(AccessToken)로 확인한 뒤 넘겨 줌
//      memberId : 요청한 회원 번호 (로그인 안 했으면 null)
//      isAdmin  : 관리자인지 (true 면 고객센터 쪽)
//    → 이 서비스는 쿠키를 직접 읽지 않고 , 받은 값으로 "할 수 있는 일인지" 만 판단
//      (나중에 AOP 로 권한 확인을 모아도 이 서비스는 고칠 필요 없음)
//  - 권한이 없거나 할 수 없는 일이면 null / false 를 돌려줌 (AuthorizationService 와 같은 방식)
//  - 메소드마다 처음부터 끝까지 한 곳에서 처리 (다른 메소드를 타고 들어가지 않게)
//    → 비슷한 코드가 여러 메소드에 반복되므로 , 규칙을 바꿀 때는 같은 번호 주석이 있는 곳을 같이 고칠 것
//    → 메시지를 저장하는 곳마다 "※ 6단계" 표시 : 웹소켓으로 바로 보내는 코드를 넣을 자리
//
//  [방 관련]     ① 내 채팅방 보기  ② 상담 시작(on)  ③ 상담 종료(off)  ④ 관리자 채팅방 목록
//  [메시지 관련] ⑤ 메시지 목록  ⑥ 읽음 처리  메시지 보내기  탈퇴 처리   ← 다음에 추가
//  =====================================================================
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

//  ---------- 보낸 사람 종류 (message.sender_type 에 들어가는 글자) ----------
//  이 글자를 다루는 곳은 이 서비스 하나뿐이라 private (다른 서비스는 이 서비스의 메소드를 부름)
    private static final String SENDER_MEMBER = "MEMBER";
    private static final String SENDER_ADMIN = "ADMIN";
    private static final String SENDER_SYSTEM = "SYSTEM";

//  ---------- 보낸 사람 이름 (message.sender_name) ----------
    private static final String SYSTEM_NAME = "시스템";     // SYSTEM 메시지의 이름
    private static final String CENTER_NAME = "고객센터";   // 자동 인사의 이름 + 회원에게 보여줄 상담원 이름

//  ---------- 자동으로 남기는 문구 ----------
    private static final String OPEN_TEXT = "상담이 시작되었습니다";
    private static final String CLOSE_BY_ADMIN_TEXT = "고객센터에서 상담을 종료했습니다";
    private static final String CLOSE_BY_MEMBER_TEXT = "회원님이 상담을 종료했습니다";
    private static final String WELCOME_TEXT = "안녕하세요, MACROSS 고객센터입니다. 무엇을 도와드릴까요?";

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;


//  =====================================================================
//  ① 내 채팅방 보기 (기업 회원) — GET /api/chat/my-room
//  - 돌려주는 것 : 방 정보 + 이번 상담 메시지 + 회원 기준 안 읽음 수
//  - 방이 없으면(상담을 한 번도 시작하지 않았으면) null
//  - 메시지는 이번 상담(opened_at 이후)만 → 종료 후 다시 시작하면 새 창처럼 보임
//  =====================================================================
    @Transactional (readOnly = true)   // 조회만 하는 메소드 → JPA 가 변경 감시를 하지 않아서 조금 더 가벼움
    public RoomDto getMyRoom(String memberId, boolean isAdmin){

    //  1. 로그인 안 했거나 관리자면 null (관리자는 "내 채팅방" 이 없음 — 관리자 화면은 목록을 씀)
        if (memberId == null || isAdmin) { return null; }

    //  2. 내 방 찾기 (없으면 null)
    //     SELECT * FROM room WHERE member_id = ?
        RoomEntity roomEntity = roomRepository.findByMemberEntityMemberId(memberId).orElse(null);
        if (roomEntity == null) { return null; }

    //  3. 이번 상담 메시지만 조회 (방이 마지막으로 켜진 시각 opened_at 이후 , 보낸 순서대로)
    //     SELECT * FROM message WHERE room_id = ? AND created_at >= ? ORDER BY message_id ASC
        List<MessageEntity> messageEntities = messageRepository.findByRoomEntityRoomIdAndCreatedAtGreaterThanEqualOrderByMessageIdAsc(
            roomEntity.getRoomId(), roomEntity.getOpenedAt());

    //  4. 메시지 엔티티 목록 → DTO 목록 (stream().map( ) 방식)
    //     회원에게 보내는 거라 상담원(ADMIN) 이름은 "고객센터" 로 바꿈 → 상담원 이름이 회원 브라우저에 아예 가지 않음
        List<MessageDto> messageDtos = messageEntities.stream().map( (messageEntity) -> {
            MessageDto messageDto = MessageDto.from(messageEntity);
            if (SENDER_ADMIN.equals(messageDto.getSenderType())) {
                messageDto.setSenderName(CENTER_NAME);
            }
            return messageDto;
        } ).toList();

    //  5. 회원 기준 안 읽은 메시지 수 = 고객센터(ADMIN)가 보낸 것 중 "회원이 마지막으로 읽은 시각" 이후에 온 것
    //     읽은 시각이 비어 있으면 상담이 켜진 시각부터 셈
    //     SELECT COUNT(*) FROM message WHERE room_id = ? AND sender_type = 'ADMIN' AND created_at > ?
        LocalDateTime readAt = roomEntity.getMemberReadAt();
        if (readAt == null) { readAt = roomEntity.getOpenedAt(); }
        Long unreadCount = messageRepository.countByRoomEntityRoomIdAndSenderTypeAndCreatedAtAfter(
            roomEntity.getRoomId(), SENDER_ADMIN, readAt);

    //  6. 방 정보에 메시지 목록 + 안 읽음 수를 담아서 반환
        RoomDto roomDto = RoomDto.from(roomEntity);
        roomDto.setMessages(messageDtos);
        roomDto.setUnreadCount(unreadCount);
        return roomDto;
    }


//  =====================================================================
//  ② 상담 시작 (on) (기업 회원) — POST /api/chat/my-room/open
//  - 방이 없으면 새로 만들고 , 종료된 방이면 다시 켬 , 이미 상담 중이면 그대로
//  - 켤 때마다 대화에 "상담이 시작되었습니다" + 고객센터 인사를 남김 (켜고 끈 이력)
//  - 돌려주는 것 : ① 과 같은 모양 (방 정보 + 이번 상담 메시지 + 안 읽음 수)
//  =====================================================================
    public RoomDto openMyRoom(String memberId, boolean isAdmin){

    //  1. 로그인 안 했거나 관리자면 null (상담은 기업 회원이 시작함)
        if (memberId == null || isAdmin) { return null; }

    //  2. 회원 확인 (없는 회원이거나 비활성 회원이면 null)
        MemberEntity memberEntity = memberRepository.findById(memberId).orElse(null);
        if (memberEntity == null || memberEntity.getStatus() == false) { return null; }

    //  3. 내 방 찾기
        RoomEntity roomEntity = roomRepository.findByMemberEntityMemberId(memberId).orElse(null);

    //  4. 방이 없거나 종료된 방일 때만 켜기 (이미 상담 중이면 4번을 건너뛰고 5번으로 → 그대로 돌려줌)
    //     이미 상담 중인 경우 : 버튼을 두 번 누르거나 탭을 두 개 연 경우
        if (roomEntity == null || roomEntity.getStatus() == false) {

            LocalDateTime now = LocalDateTime.now();

            if (roomEntity == null) {
            //  4-1. 처음 상담 → 방 만들기
            //       status 는 @Builder.Default 로 true (RoomEntity 참고)
            //       adminReadAt 을 지금으로 → 이후에 회원이 보내는 메시지부터 관리자 "안 읽음" 으로 셈
                roomEntity = RoomEntity.builder()
                    .memberEntity(memberEntity)
                    .openedAt(now)
                    .adminReadAt(now)
                    .build();
            } else {
            //  4-2. 종료된 방 → 다시 켜기 (on)
            //       opened_at 이 지금으로 바뀌므로 , 회원 화면에는 지금부터의 메시지만 보임 (새 창)
                roomEntity.setStatus(true);
                roomEntity.setOpenedAt(now);
                roomEntity.setClosedAt(null);
            }

        //  4-3. 회원 정보 복사 (상담을 시작할 때마다 그때의 정보로 → 탈퇴해도 누구와의 상담인지 남음)
            roomEntity.setManagerName(memberEntity.getManagerName());
            roomEntity.setCompanyName(memberEntity.getCompanyName());
            roomEntity.setCompanyType(memberEntity.getSignupEntity().getSignupType());

        //  4-4. 방 저장
        //       - 새 방 : INSERT (메시지를 저장하려면 room_id 가 먼저 있어야 해서 메시지보다 먼저 저장)
        //       - 기존 방 : 이미 JPA 가 관리 중인 객체라 save 없이도 메소드가 끝날 때 UPDATE 됨 (dirty checking)
        //         → 두 경우를 같은 코드로 쓰려고 save 를 호출 (기존 방에 호출해도 문제없음)
            roomEntity = roomRepository.save(roomEntity);

        //  4-5. 대화에 시작 이력 남기기 (SYSTEM 메시지)
        //       created_at 은 저장될 때 JPA 가 자동으로 채움 (@CreatedDate)
        //       INSERT INTO message (room_id , sender_type , sender_name , content , created_at) VALUES (...)
            MessageEntity openMessage = MessageEntity.builder()
                .roomEntity(roomEntity)
                .senderType(SENDER_SYSTEM)
                .senderName(SYSTEM_NAME)
                .content(OPEN_TEXT)
                .build();
            messageRepository.save(openMessage);
        //  ※ 6단계 : 여기에 웹소켓으로 바로 보내기 추가

        //  4-6. 고객센터 인사 남기기 (ADMIN 메시지)
        //       특정 상담원이 아니라 자동으로 보내는 인사라 이름을 "고객센터" 로
            MessageEntity welcomeMessage = MessageEntity.builder()
                .roomEntity(roomEntity)
                .senderType(SENDER_ADMIN)
                .senderName(CENTER_NAME)
                .content(WELCOME_TEXT)
                .build();
            messageRepository.save(welcomeMessage);
        //  ※ 6단계 : 여기에 웹소켓으로 바로 보내기 추가

        //  4-7. 회원은 방금 연 화면을 보고 있으므로 여기까지 읽은 것으로 (인사가 "안 읽음 1" 로 뜨지 않게)
        //       메시지를 저장한 "뒤" 의 시각이라 방금 저장한 메시지보다 늦음
            roomEntity.setMemberReadAt(LocalDateTime.now());
        }

    //  5. 이번 상담 메시지만 조회 (① 내 채팅방 보기의 3번과 같음)
        List<MessageEntity> messageEntities = messageRepository.findByRoomEntityRoomIdAndCreatedAtGreaterThanEqualOrderByMessageIdAsc(
            roomEntity.getRoomId(), roomEntity.getOpenedAt());

    //  6. 메시지 엔티티 목록 → DTO 목록 (회원에게 보내는 거라 상담원 이름은 "고객센터" 로 — ① 의 4번과 같음)
        List<MessageDto> messageDtos = messageEntities.stream().map( (messageEntity) -> {
            MessageDto messageDto = MessageDto.from(messageEntity);
            if (SENDER_ADMIN.equals(messageDto.getSenderType())) {
                messageDto.setSenderName(CENTER_NAME);
            }
            return messageDto;
        } ).toList();

    //  7. 회원 기준 안 읽은 메시지 수 (① 의 5번과 같음)
        LocalDateTime readAt = roomEntity.getMemberReadAt();
        if (readAt == null) { readAt = roomEntity.getOpenedAt(); }
        Long unreadCount = messageRepository.countByRoomEntityRoomIdAndSenderTypeAndCreatedAtAfter(
            roomEntity.getRoomId(), SENDER_ADMIN, readAt);

    //  8. 방 정보에 메시지 목록 + 안 읽음 수를 담아서 반환
        RoomDto roomDto = RoomDto.from(roomEntity);
        roomDto.setMessages(messageDtos);
        roomDto.setUnreadCount(unreadCount);
        return roomDto;
    }


//  =====================================================================
//  ③ 상담 종료 (off) (그 방의 회원 또는 관리자) — PUT /api/chat/rooms/{roomId}/close
//  - 종료하면 대화에 누가 종료했는지 남김
//  - 없는 방 , 남의 방 , 이미 종료된 방이면 false
//  =====================================================================
    public boolean closeRoom(Integer roomId, String memberId, boolean isAdmin){

    //  1. 방 찾기 (없으면 false)
        if (roomId == null) { return false; }
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) { return false; }

    //  2. 권한 확인 : 관리자는 모든 방 , 회원은 내 방만
    //     방 번호(room_id)는 1 , 2 , 3 ... 순서라 누구나 추측할 수 있으므로 번호만 믿지 않고 꼭 주인인지 확인
    //     - owner 가 null 이면 탈퇴한 회원의 방 → 관리자만 종료할 수 있음
    //     - getMemberId( ) 는 LAZY 라도 추가 조회 없이 번호를 꺼낼 수 있음
        if (!isAdmin) {
            MemberEntity owner = roomEntity.getMemberEntity();
            boolean isOwner = memberId != null && owner != null && memberId.equals(owner.getMemberId());
            if (!isOwner) { return false; }
        }

    //  3. 이미 종료된 방이면 false (종료 이력이 두 번 남지 않게)
        if (roomEntity.getStatus() == false) { return false; }

    //  4. 끄기 (off) — setter 로 바꾸면 메소드가 끝날 때 JPA 가 UPDATE 해 줌 (dirty checking)
        roomEntity.setStatus(false);
        roomEntity.setClosedAt(LocalDateTime.now());

    //  5. 대화에 종료 이력 남기기 (SYSTEM 메시지 , 누가 종료했는지에 따라 문구가 다름)
    //     조건 ? A : B  →  조건이 true 면 A , false 면 B (if-else 를 한 줄로 쓴 것)
        String closeText = isAdmin ? CLOSE_BY_ADMIN_TEXT : CLOSE_BY_MEMBER_TEXT;
        MessageEntity closeMessage = MessageEntity.builder()
            .roomEntity(roomEntity)
            .senderType(SENDER_SYSTEM)
            .senderName(SYSTEM_NAME)
            .content(closeText)
            .build();
        messageRepository.save(closeMessage);
    //  ※ 6단계 : 여기에 웹소켓으로 바로 보내기 추가

        return true;
    }


//  =====================================================================
//  ④ 관리자 채팅방 목록 (관리자) — GET /api/chat/rooms
//  - 정렬 : 상담 중인 방(켜진 시각이 오래된 순 = 선입선출) → 종료된 방(꺼진 시각이 최근 순)
//  - 방마다 마지막 메시지(미리보기 · "n분 전") + 관리자 기준 안 읽음 수
//  - 관리자가 아니면 null
//  =====================================================================
    @Transactional (readOnly = true)
    public List<RoomDto> getRooms(boolean isAdmin){

    //  1. 관리자만
        if (!isAdmin) { return null; }

    //  2. 상담 중인 방 먼저 , 그 아래에 종료된 방 (Repository 메소드 2개를 이어 붙임)
    //     정렬 기준이 그룹마다 달라서 쿼리 1번으로 하지 않고 2번으로 나눔
    //     SELECT * FROM room WHERE status = true  ORDER BY opened_at ASC
    //     SELECT * FROM room WHERE status = false ORDER BY closed_at DESC
        List<RoomEntity> roomEntities = new ArrayList<>();
        roomEntities.addAll(roomRepository.findByStatusTrueOrderByOpenedAtAsc());
        roomEntities.addAll(roomRepository.findByStatusFalseOrderByClosedAtDesc());

    //  3. 방 엔티티 목록 → DTO 목록 , 방마다 마지막 메시지 + 안 읽음 수 넣기
    //     ※ 방 1개당 쿼리가 2번씩(마지막 메시지 , 안 읽음 수) 나감
    //       방이 수십 ~ 수백 개일 때는 문제없고 , 아주 많아지면 그때 쿼리를 합치는 방법으로 바꾸면 됨
        List<RoomDto> result = roomEntities.stream().map( (roomEntity) -> {

        //  3-1. 방 정보
            RoomDto roomDto = RoomDto.from(roomEntity);

        //  3-2. 마지막 메시지 1개 (메시지가 없으면 null)
        //       관리자가 보는 거라 상담원 이름을 바꾸지 않고 그대로
        //       SELECT * FROM message WHERE room_id = ? ORDER BY message_id DESC LIMIT 1
            MessageEntity lastMessage = messageRepository.findFirstByRoomEntityRoomIdOrderByMessageIdDesc(roomEntity.getRoomId()).orElse(null);
            if (lastMessage != null) {
                roomDto.setLastMessage(MessageDto.from(lastMessage));
            }

        //  3-3. 관리자 기준 안 읽은 메시지 수 = 회원(MEMBER)이 보낸 것 중 "관리자가 마지막으로 읽은 시각" 이후에 온 것
        //       읽은 시각이 비어 있으면 상담이 켜진 시각부터 셈
        //       SELECT COUNT(*) FROM message WHERE room_id = ? AND sender_type = 'MEMBER' AND created_at > ?
            LocalDateTime readAt = roomEntity.getAdminReadAt();
            if (readAt == null) { readAt = roomEntity.getOpenedAt(); }
            Long unreadCount = messageRepository.countByRoomEntityRoomIdAndSenderTypeAndCreatedAtAfter(
                roomEntity.getRoomId(), SENDER_MEMBER, readAt);
            roomDto.setUnreadCount(unreadCount);

            return roomDto;
        } ).toList();

    //  4. 정렬된 목록 반환 (stream 은 순서를 그대로 지킴 → 2번에서 정한 순서 그대로)
        return result;
    }

}
