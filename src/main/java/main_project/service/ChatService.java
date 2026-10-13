package main_project.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MessageDto;
import main_project.model.dto.MessageSendDto;
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
//  - 웹소켓 실시간 보내기 : 메시지를 저장하는 곳마다 "※ 웹소켓 보내기 예약" 한 줄
//    → 저장이 확정(commit)된 "뒤" 에 Spring 이 맨 아래 ⑨ sendAfterCommit 을 자동으로 부름
//
//  [방 관련]     ① 내 채팅방 보기  ② 상담 시작(on)  ③ 상담 종료(off)  ④ 관리자 채팅방 목록
//  [메시지 관련] ⑤ 메시지 목록  ⑥ 읽음 처리  ⑦ 메시지 보내기  ⑧ 탈퇴 처리
//  [웹소켓]      ⑨ 저장이 확정된 뒤 보내기  ⑩ 구독해도 되는지 확인
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
    private static final String WITHDRAW_CLOSE_TEXT = "회원 탈퇴로 상담이 종료되었습니다";   // 상담 중에 탈퇴
    private static final String WITHDRAW_TEXT = "회원이 탈퇴했습니다";                       // 종료된 뒤에 탈퇴

//  메시지 내용 최대 글자 수 (MessageEntity 의 content 칸 길이와 같게 — AuditService 의 DETAIL_MAX_LENGTH 와 같은 방식)
    private static final int CONTENT_MAX_LENGTH = 1000;

//  관리자 권한 번호 (role 테이블 : 1 = ROLE_USER , 2 = ROLE_ADMIN — MypageService 와 같은 방식)
//  ⑦ 메시지 보내기에서 "지금도 관리자인지" 를 DB 값으로 다시 확인할 때 사용
    private static final int ROLE_ADMIN_ID = 2;

//  ---------- 웹소켓 구독 주소 (WebSocketConfig 의 구독 주소 /sub 아래) ----------
//  보내는 곳(⑨ sendAfterCommit)과 구독 검사(⑩ canSubscribe)가 같은 글자를 써야 해서 한 곳에 둠
    private static final String ADMIN_TOPIC = "/sub/chat/admin";          // 관리자 화면 (모든 방의 메시지)
    private static final String ROOM_TOPIC_PREFIX = "/sub/chat/room/";    // 회원 화면 (뒤에 방 번호 — 예: /sub/chat/room/3)

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;

//  웹소켓으로 보내는 도구 (springweb day062 MessageController 의 messageTemp 와 같은 것)
    private final SimpMessageSendingOperations messageTemplate;

//  "저장이 확정되면 보내 줘" 하고 예약하는 도구 (예약한 내용은 ⑨ sendAfterCommit 이 받음)
    private final ApplicationEventPublisher eventPublisher;


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
        //       ※ 웹소켓 보내기 예약 — 저장이 확정된 뒤 ⑨ 에서 회원 화면 · 관리자 화면으로 보냄
            eventPublisher.publishEvent(MessageDto.from(openMessage));

        //  4-6. 고객센터 인사 남기기 (ADMIN 메시지)
        //       특정 상담원이 아니라 자동으로 보내는 인사라 이름을 "고객센터" 로
            MessageEntity welcomeMessage = MessageEntity.builder()
                .roomEntity(roomEntity)
                .senderType(SENDER_ADMIN)
                .senderName(CENTER_NAME)
                .content(WELCOME_TEXT)
                .build();
            messageRepository.save(welcomeMessage);
        //       ※ 웹소켓 보내기 예약 (4-5 와 같음)
            eventPublisher.publishEvent(MessageDto.from(welcomeMessage));

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
    //     ※ 웹소켓 보내기 예약 — 회원 화면은 이 메시지의 roomStatus(false)를 보고 바로 입력창을 잠금
        eventPublisher.publishEvent(MessageDto.from(closeMessage));

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


//  =====================================================================
//  ⑤ 한 방의 메시지 목록 (그 방의 회원 또는 관리자) — GET /api/chat/rooms/{roomId}/messages
//  - 관리자 : 지난 상담까지 전부 (증거 · 교육 자료로 볼 수 있게)
//  - 회원   : 이번 상담(opened_at 이후)만 (다시 시작하면 새 창처럼)
//  - 없는 방 , 남의 방이면 null
//  =====================================================================
    @Transactional (readOnly = true)
    public List<MessageDto> getMessages(Integer roomId, String memberId, boolean isAdmin){

    //  1. 방 찾기 (없으면 null)
    //     SELECT * FROM room WHERE room_id = ?
        if (roomId == null) { return null; }
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) { return null; }

    //  2. 권한 확인 : 관리자는 모든 방 , 회원은 내 방만 (③ 상담 종료의 2번과 같음)
        if (!isAdmin) {
            MemberEntity owner = roomEntity.getMemberEntity();
            boolean isOwner = memberId != null && owner != null && memberId.equals(owner.getMemberId());
            if (!isOwner) { return null; }
        }

    //  3. 메시지 조회 (보는 사람에 따라 범위가 다름)
        List<MessageEntity> messageEntities;
        if (isAdmin) {
        //  3-1. 관리자 : 전부
        //       SELECT * FROM message WHERE room_id = ? ORDER BY message_id ASC
            messageEntities = messageRepository.findByRoomEntityRoomIdOrderByMessageIdAsc(roomEntity.getRoomId());
        } else {
        //  3-2. 회원 : 이번 상담만 (① 내 채팅방 보기의 3번과 같음)
        //       SELECT * FROM message WHERE room_id = ? AND created_at >= ? ORDER BY message_id ASC
            messageEntities = messageRepository.findByRoomEntityRoomIdAndCreatedAtGreaterThanEqualOrderByMessageIdAsc(
                roomEntity.getRoomId(), roomEntity.getOpenedAt());
        }

    //  4. 메시지 엔티티 목록 → DTO 목록
    //     회원에게 보낼 때만 상담원(ADMIN) 이름을 "고객센터" 로 바꿈 (① 의 4번과 같음)
        List<MessageDto> messageDtos = messageEntities.stream().map( (messageEntity) -> {
            MessageDto messageDto = MessageDto.from(messageEntity);
            if (!isAdmin && SENDER_ADMIN.equals(messageDto.getSenderType())) {
                messageDto.setSenderName(CENTER_NAME);
            }
            return messageDto;
        } ).toList();

        return messageDtos;
    }


//  =====================================================================
//  ⑥ 읽음 처리 (그 방의 회원 또는 관리자) — PUT /api/chat/rooms/{roomId}/read
//  - "지금까지 온 메시지를 다 읽었다" 고 시각을 기록 → 이 시각 이후에 온 상대 메시지만 "안 읽음" 으로 셈
//  - 관리자가 읽으면 admin_read_at , 회원이 읽으면 member_read_at
//    (관리자 중 누구라도 읽으면 admin_read_at 이 바뀜 — 관리자 전체가 같은 고객센터라서)
//  - 없는 방 , 남의 방이면 false
//  =====================================================================
    public boolean markRead(Integer roomId, String memberId, boolean isAdmin){

    //  1. 방 찾기 (없으면 false)
        if (roomId == null) { return false; }
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) { return false; }

    //  2. 권한 확인 : 관리자는 모든 방 , 회원은 내 방만 (③ 상담 종료의 2번과 같음)
        if (!isAdmin) {
            MemberEntity owner = roomEntity.getMemberEntity();
            boolean isOwner = memberId != null && owner != null && memberId.equals(owner.getMemberId());
            if (!isOwner) { return false; }
        }

    //  3. 읽은 시각을 지금으로 — setter 로 바꾸면 메소드가 끝날 때 JPA 가 UPDATE 해 줌 (dirty checking)
    //     UPDATE room SET admin_read_at = ? WHERE room_id = ?   (회원이면 member_read_at)
        if (isAdmin) {
            roomEntity.setAdminReadAt(LocalDateTime.now());
        } else {
            roomEntity.setMemberReadAt(LocalDateTime.now());
        }

        return true;
    }


//  =====================================================================
//  ⑦ 메시지 보내기 (그 방의 회원 또는 관리자) — 웹소켓 /pub/chat/message (6단계에서 연결)
//  - 받는 값 : 어느 방에(roomId) , 무슨 내용을(content) — MessageSendDto
//  - 누가 보냈는지는 받지 않음 → 컨트롤러가 웹소켓 연결 때 쿠키로 확인한 memberId 로 정함
//  - 관리자인지는 연결할 때 값을 믿지 않고 , 메시지마다 DB 에서 다시 확인 (3 · 4번)
//    → 연결된 채로 관리자 권한을 빼거나 비활성 처리해도 다음 메시지부터 바로 반영됨
//  - 저장하지 않는 경우 (null 반환)
//      로그인 안 함 , 내용이 비었거나 1000자 초과 , 없는 회원 · 비활성 회원 , 없는 방 , 남의 방 , 종료된 방
//  - 돌려주는 것 : 저장된 메시지 (상담원 이름 그대로 — 관리자 기준)
//  =====================================================================
    public MessageDto sendMessage(MessageSendDto messageSendDto, String memberId){

    //  1. 로그인 안 했거나 , 요청 내용이 없으면 null
        if (memberId == null) { return null; }
        if (messageSendDto == null || messageSendDto.getRoomId() == null || messageSendDto.getContent() == null) { return null; }

    //  2. 내용 검사 : 앞뒤 빈칸을 지우고 , 비었거나 1000자를 넘으면 null
    //     trim( ) : "  안녕  " → "안녕" (앞뒤 빈칸 · 줄바꿈 제거)
        String content = messageSendDto.getContent().trim();
        if (content.isEmpty() || content.length() > CONTENT_MAX_LENGTH) { return null; }

    //  3. 보낸 사람 확인 (없는 회원이거나 비활성 회원이면 null)
    //     권한 확인(6번)에 관리자인지가 필요해서 , 방보다 먼저 조회
    //     SELECT * FROM member WHERE member_id = ?
        MemberEntity senderEntity = memberRepository.findById(memberId).orElse(null);
        if (senderEntity == null || senderEntity.getStatus() == false) { return null; }

    //  4. 관리자인지 지금 DB 값으로 확인 (role 2 = 관리자)
        boolean isAdmin = senderEntity.getRoleEntity().getRoleId() == ROLE_ADMIN_ID;

    //  5. 방 찾기 (없으면 null)
    //     SELECT * FROM room WHERE room_id = ?
        RoomEntity roomEntity = roomRepository.findById(messageSendDto.getRoomId()).orElse(null);
        if (roomEntity == null) { return null; }

    //  6. 권한 확인 : 관리자는 모든 방 , 회원은 내 방만 (③ 상담 종료의 2번과 같음)
        if (!isAdmin) {
            MemberEntity owner = roomEntity.getMemberEntity();
            boolean isOwner = owner != null && memberId.equals(owner.getMemberId());
            if (!isOwner) { return null; }
        }

    //  7. 종료된 방이면 null (상담 중일 때만 대화할 수 있음 — 회원은 [상담 시작] 으로 다시 켜야 함)
        if (roomEntity.getStatus() == false) { return null; }

    //  8. 메시지 저장
    //     - 보낸 사람 종류 : 관리자면 ADMIN , 아니면 MEMBER
    //     - 보낸 사람 이름 : 보낸 그 순간의 이름을 복사 (관리자면 상담원 이름 → 누가 답했는지 남음)
    //     INSERT INTO message (room_id , sender_type , sender_name , content , created_at) VALUES (...)
        String senderType = isAdmin ? SENDER_ADMIN : SENDER_MEMBER;
        MessageEntity messageEntity = MessageEntity.builder()
            .roomEntity(roomEntity)
            .senderType(senderType)
            .senderName(senderEntity.getManagerName())
            .content(content)
            .build();
        messageEntity = messageRepository.save(messageEntity);
    //     ※ 웹소켓 보내기 예약 — 보낸 사람 화면에도 이 경로로 말풍선이 뜸 (보낸 사람도 같은 주소를 구독 중)
        eventPublisher.publishEvent(MessageDto.from(messageEntity));

    //  9. 보낸 사람은 자기 메시지까지 읽은 것으로 (내가 보낸 메시지 앞에 상대가 보낸 것도 이미 화면에서 봤으므로)
    //     메시지를 저장한 "뒤" 의 시각이라 방금 저장한 메시지보다 늦음
        if (isAdmin) {
            roomEntity.setAdminReadAt(LocalDateTime.now());
        } else {
            roomEntity.setMemberReadAt(LocalDateTime.now());
        }

    //  10. 저장된 메시지 반환
        return MessageDto.from(messageEntity);
    }


//  =====================================================================
//  ⑧ 회원 탈퇴 처리 — MypageService.withdraw( ) 가 회원을 삭제하기 "직전" 에 부름
//  - 채팅방과 대화는 지우지 않고 남김 (감사 로그처럼 증거 · 교육 자료)
//  - 채팅방의 member_id 를 NULL 로 비움
//      → room.member_id 는 FK 라서 , 비우지 않고 회원을 지우면 DB 가 삭제를 거부함 (감사 로그를 옮기는 이유와 같음)
//      → 누구와의 상담이었는지는 채팅방의 회원 정보 복사본(manager_name , company_name)에 남아 있음
//  - 상담 중이었으면 종료하고 , 대화에 탈퇴 이력을 남김
//  - MypageService 와 같은 트랜잭션 안에서 실행됨
//      → 탈퇴가 중간에 실패하면 여기서 바꾼 것도 같이 취소됨 (회원은 남았는데 방만 비워지는 일이 없음)
//  =====================================================================
    public void closeRoomByWithdrawal(String memberId){

    //  1. 회원 번호가 없으면 할 일 없음
        if (memberId == null) { return; }

    //  2. 탈퇴하는 회원의 채팅방 찾기 (상담을 한 번도 안 했으면 방이 없음 → 할 일 없음)
    //     SELECT * FROM room WHERE member_id = ?
        RoomEntity roomEntity = roomRepository.findByMemberEntityMemberId(memberId).orElse(null);
        if (roomEntity == null) { return; }

    //  3. 상담 중이었으면 끄기 (off)
    //     대화에 남길 문구도 상황에 따라 다르게 (상담 중에 탈퇴 / 이미 종료된 뒤에 탈퇴)
        String withdrawText;
        if (roomEntity.getStatus()) {
            roomEntity.setStatus(false);
            roomEntity.setClosedAt(LocalDateTime.now());
            withdrawText = WITHDRAW_CLOSE_TEXT;
        } else {
            withdrawText = WITHDRAW_TEXT;
        }

    //  4. 대화에 탈퇴 이력 남기기 (SYSTEM 메시지)
    //     INSERT INTO message (room_id , sender_type , sender_name , content , created_at) VALUES (...)
        MessageEntity withdrawMessage = MessageEntity.builder()
            .roomEntity(roomEntity)
            .senderType(SENDER_SYSTEM)
            .senderName(SYSTEM_NAME)
            .content(withdrawText)
            .build();
        messageRepository.save(withdrawMessage);
    //     ※ 웹소켓 보내기 예약 — 탈퇴(회원 삭제)까지 모두 성공해야 보내짐 (실패하면 예약도 취소)
        eventPublisher.publishEvent(MessageDto.from(withdrawMessage));

    //  5. 채팅방에서 회원 연결 끊기 (member_id → NULL)
    //     UPDATE room SET member_id = NULL , status = false ... WHERE room_id = ?
    //     ※ 이 UPDATE 는 MypageService 의 회원 DELETE 보다 먼저 실행됨
    //       (JPA 는 모아 둔 SQL 을 실행할 때 INSERT → UPDATE → DELETE 순서로 실행)
        roomEntity.setMemberEntity(null);
    }


//  =====================================================================
//  ⑨ 웹소켓으로 보내기 — 위의 "※ 웹소켓 보내기 예약" 이 있으면 Spring 이 자동으로 부름 (직접 부르지 않음)
//
//  - 왜 저장한 자리에서 바로 보내지 않고 예약하는가
//      이 서비스의 메소드들은 @Transactional 이라 , 메소드가 "끝날 때" DB 저장이 확정(commit)됨
//      저장한 자리에서 바로 보내면 , 확정되기 "전" 에 화면이 알림을 받아서 목록을 다시 조회했을 때
//      새 메시지가 아직 안 보일 수 있음
//        바로 보내기 : 알림 → 화면이 목록 조회 (아직 commit 전이라 옛날 목록) → commit
//        예약해서 보내기 : commit → 알림 → 화면이 목록 조회 (새 목록)
//      또 저장이 실패해서 취소(rollback)되면 예약도 같이 취소됨 → 저장 안 된 메시지가 화면에 뜨는 일이 없음
//
//  - @TransactionalEventListener : 예약된 값(MessageDto)을 받아서 , 저장이 확정된 "뒤" 에 실행
//    (AuditService 의 @EventListener(ApplicationReadyEvent.class) 처럼 Spring 이 알아서 부르는 메소드)
//  - @Transactional (propagation = NOT_SUPPORTED) : 이 메소드는 트랜잭션 없이 실행
//    (DB 작업 없이 보내기만 함 + Spring 규칙 : 확정 뒤에 실행되는 메소드는 트랜잭션 설정을 따로 표시해야 함)
//
//  - 보내는 곳 2군데
//      /sub/chat/admin         : 관리자 화면 (모든 방 , 상담원 이름 그대로)
//      /sub/chat/room/{roomId} : 그 방의 회원 화면 (상담원 이름은 "고객센터" 로)
//  =====================================================================
    @TransactionalEventListener
    @Transactional (propagation = Propagation.NOT_SUPPORTED)
    public void sendAfterCommit(MessageDto messageDto){

    //  1. 관리자 화면으로 보내기 (상담원 이름 그대로)
    //     convertAndSend( 보낼 주소 , 내용물 ) : day062 와 같음 — 내용물은 JSON 글자로 바뀌어서 전달됨
        messageTemplate.convertAndSend(ADMIN_TOPIC, messageDto);

    //  2. 회원에게 보낼 때는 상담원(ADMIN) 이름을 "고객센터" 로 바꿈 (① 의 4번과 같은 규칙)
    //     1번에서 이미 JSON 글자로 바뀌어 보내졌기 때문에 , 지금 바꿔도 관리자에게 간 내용은 그대로
        if (SENDER_ADMIN.equals(messageDto.getSenderType())) {
            messageDto.setSenderName(CENTER_NAME);
        }

    //  3. 그 방의 회원 화면으로 보내기
        messageTemplate.convertAndSend(ROOM_TOPIC_PREFIX + messageDto.getRoomId(), messageDto);
    }


//  =====================================================================
//  ⑩ 구독해도 되는지 확인 — 웹소켓으로 구독할 때마다 ChatChannelInterceptor 가 부름
//  - 관리자 주소 /sub/chat/admin          : 관리자만
//  - 방 주소     /sub/chat/room/{roomId}  : 그 방의 회원 또는 관리자 (③ 상담 종료의 2번과 같은 규칙)
//  - 그 밖의 주소 : 아무도 안 됨 (화면이 쓰는 구독 주소는 위 2개뿐)
//  - 관리자인지 · 활성 회원인지는 구독할 때마다 DB 에서 확인 (⑦ 메시지 보내기의 3 · 4번과 같은 방식)
//  - 돌려주는 것 : 구독해도 되면 true
//  =====================================================================
    @Transactional (readOnly = true)
    public boolean canSubscribe(String destination, String memberId){

    //  1. 로그인 안 했거나 , 주소가 없으면 false
        if (memberId == null || destination == null) { return false; }

    //  2. 회원 확인 (없는 회원이거나 비활성 회원이면 false)
    //     SELECT * FROM member WHERE member_id = ?
        MemberEntity memberEntity = memberRepository.findById(memberId).orElse(null);
        if (memberEntity == null || memberEntity.getStatus() == false) { return false; }

    //  3. 관리자인지 지금 DB 값으로 확인 (role 2 = 관리자)
        boolean isAdmin = memberEntity.getRoleEntity().getRoleId() == ROLE_ADMIN_ID;

    //  4. 관리자 주소 : 관리자만
        if (destination.equals(ADMIN_TOPIC)) { return isAdmin; }

    //  5. 방 주소가 아니면 false
        if (!destination.startsWith(ROOM_TOPIC_PREFIX)) { return false; }

    //  6. 주소 끝의 방 번호 꺼내기 ("/sub/chat/room/3" → 3)
    //     숫자가 아니면 (예: "/sub/chat/room/abc") Integer.valueOf 가 오류를 내므로 false
        Integer roomId;
        try {
            roomId = Integer.valueOf(destination.substring(ROOM_TOPIC_PREFIX.length()));
        } catch (NumberFormatException e) {
            return false;
        }

    //  7. 방 찾기 (없으면 false)
    //     SELECT * FROM room WHERE room_id = ?
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) { return false; }

    //  8. 권한 확인 : 관리자는 모든 방 , 회원은 내 방만 (탈퇴해서 주인이 없는 방이면 false)
        if (isAdmin) { return true; }
        MemberEntity owner = roomEntity.getMemberEntity();
        return owner != null && memberId.equals(owner.getMemberId());
    }

}
