package main_project.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main_project.model.dto.MessageDto;
import main_project.model.dto.MessageSendDto;
import main_project.model.dto.RoomDto;
import main_project.model.entity.MemberEntity;
import main_project.model.entity.MessageEntity;
import main_project.model.entity.RoomEntity;
import main_project.model.entity.SenderType;
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
//  - 메시지 저장 · 실시간 보내기 : 메시지를 만드는 곳마다 "※ 메시지 남기기 예약" 한 줄
//    → 방 변경 등이 확정(commit)된 "뒤" 에 Spring 이 ⑨ sendAfterCommit 을 자동으로 부름
//    → ⑨ 가 Redis Stream 에 메시지를 추가(XADD)하고 바로 웹소켓으로 보냄 — 메시지마다 DB 에 가지 않음
//
//  - 배치 저장 (write-behind) : 메시지는 Redis Stream(chat:stream)에 먼저 쌓이고 ,
//    ⑬ flushMessages 가 3초마다 모아서 DB 에 INSERT 1번으로 넣음
//      메시지 번호 : Redis 번호표 chat:msg:seq (INCR) — DB 에 들어가기 전부터 번호가 있어야 화면이 순서 · 중복을 앎
//      보낸 시각   : 서버가 받은 순간 (DB 에 들어간 시각이 아님)
//      메시지 목록 : DB(옮긴 것) + Stream(아직 안 옮긴 최근 몇 초)을 합쳐서 돌려줌 (① ② ⑤)
//    ※ 아직 DB 를 바로 세는 곳 (최대 3초 늦게 반영) : 안 읽음 수 (① ② ④)
//
//  - Redis 캐시 (cache-aside) : 메시지 · 구독 · 읽음 처리마다 하던 회원 · 방 SELECT 를 줄임
//      chat:member:{회원번호} = { status , admin , name }   60초 보관
//      chat:room:{방번호}     = { owner , status }          60초 보관
//    → Redis 에 있으면 DB 에 가지 않고 , 없으면 DB 에서 읽어서 Redis 에 넣어 둠 (서버 시작 때 미리 넣지 않음)
//    → 값이 바뀌면 저장이 확정된 뒤 캐시를 지움 → 다음 요청이 DB 에서 새 값을 읽음
//        방 : 상담 시작 · 종료 · 탈퇴 (모두 SYSTEM 메시지를 남기므로 ⑨ 에서 지움)
//        회원 : 권한 · 상태 · 이름 변경 , 탈퇴 (⑪ forgetMember 를 부름)
//      혹시 지우지 못해도 60초가 지나면 저절로 사라짐 (최대 60초 늦게 반영)
//
//  [방 관련]     ① 내 채팅방 보기  ② 상담 시작(on)  ③ 상담 종료(off)  ④ 관리자 채팅방 목록
//  [메시지 관련] ⑤ 메시지 목록  ⑥ 읽음 처리  ⑦ 메시지 보내기  ⑧ 탈퇴 처리
//  [웹소켓]      ⑨ 확정된 뒤 Stream 에 남기고 보내기  ⑩ 구독해도 되는지 확인
//  [캐시]        ⑪ 회원 캐시 지우기  ⑫ 서버 시작 때 채팅 Redis 준비
//  [배치 저장]   ⑬ Stream → DB 옮기기 (3초마다)
//  =====================================================================
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

//  ---------- 보낸 사람 종류 (message.sender_type) ----------
//  글자 상수 대신 enum SenderType 사용 (model/entity/SenderType.java) : SenderType.MEMBER / ADMIN / SYSTEM
//  → 3가지 말고는 넣을 수 없고 , 오타는 컴파일할 때 바로 빨간 줄 , 비교는 == 로

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

//  ---------- 웹소켓 구독 주소 (WebSocketConfig 의 구독 주소 /sub 아래) ----------
//  보내는 곳(⑨ sendAfterCommit)과 구독 검사(⑩ canSubscribe)가 같은 글자를 써야 해서 한 곳에 둠
    private static final String ADMIN_TOPIC = "/sub/chat/admin";          // 관리자 화면 (모든 방의 메시지)
    private static final String ROOM_TOPIC_PREFIX = "/sub/chat/room/";    // 회원 화면 (뒤에 방 번호 — 예: /sub/chat/room/3)

//  ---------- Redis 캐시 키 (RedisTokenService 의 "RT:" 처럼 앞글자로 구분) ----------
//  채팅이 쓰는 Redis 키는 모두 "chat:" 으로 시작 → ⑫ 에서 한 번에 비울 수 있음
    private static final String MEMBER_CACHE_PREFIX = "chat:member:";   // 뒤에 회원 번호
    private static final String ROOM_CACHE_PREFIX = "chat:room:";       // 뒤에 방 번호
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);   // 캐시 보관 시간

//  ---------- 배치 저장 (Redis Stream → DB) ----------
    private static final String STREAM_KEY = "chat:stream";        // 아직 DB 로 안 옮긴 메시지가 쌓이는 곳 (모든 방 공용)
    private static final String SEQ_KEY = "chat:msg:seq";           // 메시지 번호표 (INCR 할 때마다 1 , 2 , 3 ...)
    private static final String FLUSH_GROUP = "chat-flush";         // Stream 소비자 그룹 이름 (어디까지 옮겼는지 Redis 가 기억)
    private static final String FLUSH_CONSUMER = "server-1";        // 소비자 이름 (서버 1대라서 하나)
    private static final int FLUSH_BATCH_SIZE = 500;                // 한 번에 옮길 최대 개수

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;

//  웹소켓으로 보내는 도구 (springweb day062 MessageController 의 messageTemp 와 같은 것)
    private final SimpMessageSendingOperations messageTemplate;

//  "저장이 확정되면 보내 줘" 하고 예약하는 도구 (예약한 내용은 ⑨ sendAfterCommit 이 받음)
    private final ApplicationEventPublisher eventPublisher;

//  Redis 조작 도구 (RedisTokenService 와 같은 것) — 회원 · 방 캐시 , 메시지 Stream , 번호표
    private final StringRedisTemplate stringRedisTemplate;

//  SQL 을 직접 실행하는 도구 (MemberPiiMigrationRunner 와 같은 것) — 메시지 여러 개를 INSERT 1번으로 넣을 때 (⑬)
    private final JdbcTemplate jdbcTemplate;

//  ⑫ 에서 Stream · 소비자 그룹 준비가 끝났는지 (끝나기 전에 ⑬ 이 먼저 돌면 그룹이 없어서 오류가 나므로)
//  volatile : 다른 스레드(⑬ 을 돌리는 스케줄러)가 바뀐 값을 바로 보게 함
    private volatile boolean streamReady = false;


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

    //  3. 이번 상담 메시지 ① — DB 로 옮긴 것 (방이 마지막으로 켜진 시각 opened_at 이후 , 보낸 순서대로)
    //     SELECT * FROM message WHERE room_id = ? AND created_at >= ? ORDER BY message_id ASC
    //     new ArrayList<>( ) : toList( ) 로 만든 목록은 더할 수 없어서 , 4번에서 더할 수 있는 목록으로 옮겨 담음
        List<MessageDto> messageDtos = new ArrayList<>(messageRepository.findByRoomEntityRoomIdAndCreatedAtGreaterThanEqualOrderByMessageIdAsc(
            roomEntity.getRoomId(), roomEntity.getOpenedAt()).stream().map( (messageEntity) -> MessageDto.from(messageEntity) ).toList());

    //  4. 이번 상담 메시지 ② — 아직 DB 로 안 옮긴 것 (Redis Stream , 최근 3초 정도라 몇 개 안 됨) 중 이 방 · 이번 상담 것
    //     XRANGE chat:stream - +   ( - + : 처음부터 끝까지)
    //     방금 옮겨서 DB · Stream 양쪽에 다 있는 메시지는 번호(savedIds)로 걸러서 한 번만 넣음
    //     → 다 넣은 뒤 번호순으로 다시 정렬 (Comparator.comparing : 메시지 번호가 작은 것부터)
        Set<Integer> savedIds = messageDtos.stream().map( (messageDto) -> messageDto.getMessageId() ).collect(Collectors.toSet());
        stringRedisTemplate.opsForStream().range(STREAM_KEY, Range.unbounded()).forEach( (record) -> {
            MessageDto pending = MessageDto.fromStream(record.getValue());
            boolean thisSession = pending.getRoomId().equals(roomEntity.getRoomId()) && !pending.getCreatedAt().isBefore(roomEntity.getOpenedAt());
            if (thisSession && !savedIds.contains(pending.getMessageId())) {
                pending.setRoomStatus(roomEntity.getStatus());
                messageDtos.add(pending);
            }
        } );
        messageDtos.sort(Comparator.comparing( (messageDto) -> messageDto.getMessageId() ));

    //  5. 회원에게 보내는 거라 상담원(ADMIN) 이름은 "고객센터" 로 바꿈 → 상담원 이름이 회원 브라우저에 아예 가지 않음
        messageDtos.forEach( (messageDto) -> {
            if (messageDto.getSenderType() == SenderType.ADMIN) { messageDto.setSenderName(CENTER_NAME); }
        } );

    //  6. 회원 기준 안 읽은 메시지 수 = 고객센터(ADMIN)가 보낸 것 중 "회원이 마지막으로 읽은 시각" 이후에 온 것
    //     읽은 시각이 비어 있으면 상담이 켜진 시각부터 셈
    //     ※ DB 만 세므로 아직 안 옮긴 최근 3초의 메시지는 빠짐 (화면은 웹소켓으로 받을 때 +1 해서 실시간으로 맞음)
    //     SELECT COUNT(*) FROM message WHERE room_id = ? AND sender_type = 'ADMIN' AND created_at > ?
        LocalDateTime readAt = roomEntity.getMemberReadAt();
        if (readAt == null) { readAt = roomEntity.getOpenedAt(); }
        Long unreadCount = messageRepository.countByRoomEntityRoomIdAndSenderTypeAndCreatedAtAfter(
            roomEntity.getRoomId(), SenderType.ADMIN, readAt);

    //  7. 방 정보에 메시지 목록 + 안 읽음 수를 담아서 반환
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
    //     createdDtos : 이번에 새로 만든 메시지 (아직 DB 에도 Stream 에도 없음 — 확정된 뒤 ⑨ 가 Stream 에 넣음) → 5번에서 응답에 같이 넣음
        List<MessageDto> createdDtos = new ArrayList<>();
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
        //       - 번호 : Redis 번호표 — increment( ) = INCR chat:msg:seq (1 올리고 올린 값을 돌려줌)
        //       - 시각 : 지금 (방이 켜진 시각 opened_at 과 같음 → "이번 상담" 에 포함됨)
        //       - DB 에는 바로 넣지 않음 → ⑨ 가 Stream 에 남기고 , ⑬ 이 몇 초 뒤에 DB 로 옮김
            MessageDto openMessage = MessageDto.builder()
                .messageId(stringRedisTemplate.opsForValue().increment(SEQ_KEY).intValue())
                .roomId(roomEntity.getRoomId())
                .senderType(SenderType.SYSTEM)
                .senderName(SYSTEM_NAME)
                .content(OPEN_TEXT)
                .createdAt(now)
                .roomStatus(true)
                .build();
        //       ※ 메시지 남기기 예약 — 방 저장이 확정된 뒤 ⑨ 가 Stream 에 남기고 회원 화면 · 관리자 화면으로 보냄
            eventPublisher.publishEvent(openMessage);
            createdDtos.add(openMessage);

        //  4-6. 고객센터 인사 남기기 (ADMIN 메시지 , 4-5 와 같은 방법)
        //       특정 상담원이 아니라 자동으로 보내는 인사라 이름을 "고객센터" 로
            MessageDto welcomeMessage = MessageDto.builder()
                .messageId(stringRedisTemplate.opsForValue().increment(SEQ_KEY).intValue())
                .roomId(roomEntity.getRoomId())
                .senderType(SenderType.ADMIN)
                .senderName(CENTER_NAME)
                .content(WELCOME_TEXT)
                .createdAt(now)
                .roomStatus(true)
                .build();
        //       ※ 메시지 남기기 예약 (4-5 와 같음)
            eventPublisher.publishEvent(welcomeMessage);
            createdDtos.add(welcomeMessage);

        //  4-7. 회원은 방금 연 화면을 보고 있으므로 여기까지 읽은 것으로 (인사가 "안 읽음 1" 로 뜨지 않게)
        //       메시지 시각(now) "뒤" 의 시각이라 방금 만든 메시지보다 늦음
            roomEntity.setMemberReadAt(LocalDateTime.now());
        }

    //  5. 이번 상담 메시지 = DB + Stream + 방금 만든 것 (① 내 채팅방 보기의 3 · 4번과 같음)
    //     openedRoom : 람다( ) -> { } 안에서는 값이 바뀌지 않는 변수만 쓸 수 있어서 옮겨 담음 (roomEntity 는 4번에서 바뀜)
        RoomEntity openedRoom = roomEntity;
        List<MessageDto> messageDtos = new ArrayList<>(messageRepository.findByRoomEntityRoomIdAndCreatedAtGreaterThanEqualOrderByMessageIdAsc(
            openedRoom.getRoomId(), openedRoom.getOpenedAt()).stream().map( (messageEntity) -> MessageDto.from(messageEntity) ).toList());
        Set<Integer> savedIds = messageDtos.stream().map( (messageDto) -> messageDto.getMessageId() ).collect(Collectors.toSet());
        stringRedisTemplate.opsForStream().range(STREAM_KEY, Range.unbounded()).forEach( (record) -> {
            MessageDto pending = MessageDto.fromStream(record.getValue());
            boolean thisSession = pending.getRoomId().equals(openedRoom.getRoomId()) && !pending.getCreatedAt().isBefore(openedRoom.getOpenedAt());
            if (thisSession && !savedIds.contains(pending.getMessageId())) {
                pending.setRoomStatus(openedRoom.getStatus());
                messageDtos.add(pending);
            }
        } );
        messageDtos.addAll(createdDtos);
        messageDtos.sort(Comparator.comparing( (messageDto) -> messageDto.getMessageId() ));

    //  6. 회원에게 보내는 거라 상담원 이름은 "고객센터" 로 (① 의 5번과 같음)
        messageDtos.forEach( (messageDto) -> {
            if (messageDto.getSenderType() == SenderType.ADMIN) { messageDto.setSenderName(CENTER_NAME); }
        } );

    //  7. 회원 기준 안 읽은 메시지 수 (① 의 6번과 같음)
        LocalDateTime readAt = openedRoom.getMemberReadAt();
        if (readAt == null) { readAt = openedRoom.getOpenedAt(); }
        Long unreadCount = messageRepository.countByRoomEntityRoomIdAndSenderTypeAndCreatedAtAfter(
            openedRoom.getRoomId(), SenderType.ADMIN, readAt);

    //  8. 방 정보에 메시지 목록 + 안 읽음 수를 담아서 반환
        RoomDto roomDto = RoomDto.from(openedRoom);
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
    //     번호는 Redis 번호표 , 시각은 지금 (② 상담 시작의 4-5 와 같은 방법)
        String closeText = isAdmin ? CLOSE_BY_ADMIN_TEXT : CLOSE_BY_MEMBER_TEXT;
        MessageDto closeMessage = MessageDto.builder()
            .messageId(stringRedisTemplate.opsForValue().increment(SEQ_KEY).intValue())
            .roomId(roomEntity.getRoomId())
            .senderType(SenderType.SYSTEM)
            .senderName(SYSTEM_NAME)
            .content(closeText)
            .createdAt(LocalDateTime.now())
            .roomStatus(false)
            .build();
    //     ※ 메시지 남기기 예약 — 회원 화면은 이 메시지의 roomStatus(false)를 보고 바로 입력창을 잠금
        eventPublisher.publishEvent(closeMessage);

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

    //  2-1. 아직 DB 로 안 옮긴 메시지(Stream) 중 방마다 가장 최근 것 (방 번호 → 메시지)
    //       Stream 은 한 번만 읽고 , 3-2 에서 방마다 꺼내 씀
    //       Stream 은 보낸 순서대로 쌓이므로 같은 방이면 뒤에 나온 것(put 으로 덮어쓴 것)이 더 최근
        Map<Integer, MessageDto> pendingLast = new HashMap<>();
        stringRedisTemplate.opsForStream().range(STREAM_KEY, Range.unbounded()).forEach( (record) -> {
            MessageDto pending = MessageDto.fromStream(record.getValue());
            pendingLast.put(pending.getRoomId(), pending);
        } );

    //  3. 방 엔티티 목록 → DTO 목록 , 방마다 마지막 메시지 + 안 읽음 수 넣기
    //     ※ 방 1개당 쿼리가 2번씩(마지막 메시지 , 안 읽음 수) 나감
    //       방이 수십 ~ 수백 개일 때는 문제없고 , 아주 많아지면 그때 쿼리를 합치는 방법으로 바꾸면 됨
        List<RoomDto> result = roomEntities.stream().map( (roomEntity) -> {

        //  3-1. 방 정보
            RoomDto roomDto = RoomDto.from(roomEntity);

        //  3-2. 마지막 메시지 1개 (메시지가 없으면 null)
        //       DB 의 마지막 메시지와 Stream 의 마지막 메시지 중 번호가 큰 것 (Stream 쪽이 더 최근이면 아직 DB 에 없는 것)
        //       관리자가 보는 거라 상담원 이름을 바꾸지 않고 그대로
        //       SELECT * FROM message WHERE room_id = ? ORDER BY message_id DESC LIMIT 1
            MessageEntity lastMessage = messageRepository.findFirstByRoomEntityRoomIdOrderByMessageIdDesc(roomEntity.getRoomId()).orElse(null);
            MessageDto lastPending = pendingLast.get(roomEntity.getRoomId());
            if (lastPending != null && (lastMessage == null || lastPending.getMessageId() > lastMessage.getMessageId())) {
                lastPending.setRoomStatus(roomEntity.getStatus());
                roomDto.setLastMessage(lastPending);
            } else if (lastMessage != null) {
                roomDto.setLastMessage(MessageDto.from(lastMessage));
            }

        //  3-3. 관리자 기준 안 읽은 메시지 수 = 회원(MEMBER)이 보낸 것 중 "관리자가 마지막으로 읽은 시각" 이후에 온 것
        //       읽은 시각이 비어 있으면 상담이 켜진 시각부터 셈
        //       ※ DB 만 세므로 아직 안 옮긴 최근 3초의 메시지는 빠짐 (화면은 웹소켓으로 받을 때 +1 해서 실시간으로 맞음)
        //       SELECT COUNT(*) FROM message WHERE room_id = ? AND sender_type = 'MEMBER' AND created_at > ?
            LocalDateTime readAt = roomEntity.getAdminReadAt();
            if (readAt == null) { readAt = roomEntity.getOpenedAt(); }
            Long unreadCount = messageRepository.countByRoomEntityRoomIdAndSenderTypeAndCreatedAtAfter(
                roomEntity.getRoomId(), SenderType.MEMBER, readAt);
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

    //  3. DB 로 옮긴 메시지 조회 (보는 사람에 따라 범위가 다름)
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
        List<MessageDto> messageDtos = new ArrayList<>(messageEntities.stream().map( (messageEntity) -> MessageDto.from(messageEntity) ).toList());

    //  4. + 아직 DB 로 안 옮긴 메시지 (Stream) 중 이 방 것 (① 내 채팅방 보기의 4번과 같음)
    //     관리자는 전부 , 회원은 이번 상담(opened_at 이후)만
        Set<Integer> savedIds = messageDtos.stream().map( (messageDto) -> messageDto.getMessageId() ).collect(Collectors.toSet());
        stringRedisTemplate.opsForStream().range(STREAM_KEY, Range.unbounded()).forEach( (record) -> {
            MessageDto pending = MessageDto.fromStream(record.getValue());
            boolean inRange = pending.getRoomId().equals(roomEntity.getRoomId())
                && (isAdmin || !pending.getCreatedAt().isBefore(roomEntity.getOpenedAt()));
            if (inRange && !savedIds.contains(pending.getMessageId())) {
                pending.setRoomStatus(roomEntity.getStatus());
                messageDtos.add(pending);
            }
        } );
        messageDtos.sort(Comparator.comparing( (messageDto) -> messageDto.getMessageId() ));

    //  5. 회원에게 보낼 때만 상담원(ADMIN) 이름을 "고객센터" 로 바꿈 (① 의 5번과 같음)
        messageDtos.forEach( (messageDto) -> {
            if (!isAdmin && messageDto.getSenderType() == SenderType.ADMIN) { messageDto.setSenderName(CENTER_NAME); }
        } );

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

    //  1. 방 찾기 — Redis 캐시 먼저 , 없으면 DB (⑦ 메시지 보내기의 5번과 같음)
        if (roomId == null) { return false; }
        String roomKey = ROOM_CACHE_PREFIX + roomId;
        Map<Object, Object> roomCache = stringRedisTemplate.opsForHash().entries(roomKey);
        if (roomCache.isEmpty()) {
            RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
            if (roomEntity == null) { return false; }
            MemberEntity owner = roomEntity.getMemberEntity();
            roomCache = Map.of(
                "owner", owner == null ? "" : owner.getMemberId(),
                "status", roomEntity.getStatus() ? "1" : "0");
            stringRedisTemplate.opsForHash().putAll(roomKey, roomCache);
            stringRedisTemplate.expire(roomKey, CACHE_TTL);
        }

    //  2. 권한 확인 : 관리자는 모든 방 , 회원은 내 방만 (③ 상담 종료의 2번과 같음)
    //     owner 가 "" 이면 탈퇴한 회원의 방 → 어떤 회원 번호와도 같지 않음
        if (!isAdmin) {
            boolean isOwner = memberId != null && memberId.equals(roomCache.get("owner"));
            if (!isOwner) { return false; }
        }

    //  3. 읽은 시각을 지금으로 — 방을 SELECT 하지 않고 그 칸만 바로 UPDATE (RoomRepository 의 updateXxxReadAt)
    //     UPDATE room SET admin_read_at = ? , updated_at = ? WHERE room_id = ?   (회원이면 member_read_at)
        if (isAdmin) {
            roomRepository.updateAdminReadAt(roomId, LocalDateTime.now());
        } else {
            roomRepository.updateMemberReadAt(roomId, LocalDateTime.now());
        }

        return true;
    }


//  =====================================================================
//  ⑦ 메시지 보내기 (그 방의 회원 또는 관리자) — 웹소켓 /pub/chat/message (6단계에서 연결)
//  - 받는 값 : 어느 방에(roomId) , 무슨 내용을(content) — MessageSendDto
//  - 누가 보냈는지는 받지 않음 → 컨트롤러가 웹소켓 연결 때 쿠키로 확인한 memberId 로 정함
//  - 관리자인지는 연결할 때 값을 믿지 않고 , 메시지마다 다시 확인 (3 · 4번)
//    → 연결된 채로 관리자 권한을 빼거나 비활성 처리해도 다음 메시지부터 반영됨 (바꿀 때 ⑪ 이 캐시를 지움)
//  - 회원 · 방 확인은 Redis 캐시로 (3 · 5번) , 저장은 Redis Stream 으로 (8번 → ⑨) , DB 에는 ⑬ 이 몇 초 뒤 모아서
//    → 메시지 1개당 SQL : 1단계 전 4개 → 1단계 2개 → 지금 0개 (캐시가 비어 있을 때만 SELECT)
//  - 저장하지 않는 경우 (null 반환)
//      로그인 안 함 , 내용이 비었거나 1000자 초과 , 없는 회원 · 비활성 회원 , 없는 방 , 남의 방 , 종료된 방
//  - 돌려주는 것 : 만든 메시지 (컨트롤러는 쓰지 않음 — 화면에는 ⑨ 가 웹소켓으로 보냄)
//  =====================================================================
    public MessageDto sendMessage(MessageSendDto messageSendDto, String memberId){

    //  1. 로그인 안 했거나 , 요청 내용이 없으면 null
        if (memberId == null) { return null; }
        if (messageSendDto == null || messageSendDto.getRoomId() == null || messageSendDto.getContent() == null) { return null; }

    //  2. 내용 검사 : 앞뒤 빈칸을 지우고 , 비었거나 1000자를 넘으면 null
    //     trim( ) : "  안녕  " → "안녕" (앞뒤 빈칸 · 줄바꿈 제거)
        String content = messageSendDto.getContent().trim();
        if (content.isEmpty() || content.length() > CONTENT_MAX_LENGTH) { return null; }

    //  3. 보낸 사람 확인 — Redis 캐시 먼저 , 없으면 DB (cache-aside)
    //     권한 확인(6번)에 관리자인지가 필요해서 , 방보다 먼저 확인
    //     Redis : chat:member:{회원번호} = { status : "1"(활성) / "0" , admin : "1"(관리자) / "0" , name : 담당자 이름 }
    //     Redis 의 Hash 는 글자만 담을 수 있어서 true / false 를 "1" / "0" 으로 저장
        String memberKey = MEMBER_CACHE_PREFIX + memberId;
        Map<Object, Object> memberCache = stringRedisTemplate.opsForHash().entries(memberKey);   // 없으면 빈 Map
        if (memberCache.isEmpty()) {
        //  3-1. 캐시에 없음 → DB 조회 (없는 회원이면 null)
        //       SELECT * FROM member WHERE member_id = ?
            MemberEntity senderEntity = memberRepository.findById(memberId).orElse(null);
            if (senderEntity == null) { return null; }
        //  3-2. Redis 에 60초 보관 → 60초 안에 오는 메시지 · 구독은 DB 에 가지 않음
        //       관리자인지는 MemberEntity.isAdmin( ) — role 2 = 관리자
            memberCache = Map.of(
                "status", senderEntity.getStatus() ? "1" : "0",
                "admin", senderEntity.isAdmin() ? "1" : "0",
                "name", senderEntity.getManagerName());
            stringRedisTemplate.opsForHash().putAll(memberKey, memberCache);
            stringRedisTemplate.expire(memberKey, CACHE_TTL);
        }
    //  3-3. 비활성 회원이면 null
        if (!"1".equals(memberCache.get("status"))) { return null; }

    //  4. 관리자인지 · 보낸 사람 이름 꺼내기
        boolean isAdmin = "1".equals(memberCache.get("admin"));
        String senderName = (String) memberCache.get("name");

    //  5. 방 확인 — Redis 캐시 먼저 , 없으면 DB (3번과 같은 방식)
    //     Redis : chat:room:{방번호} = { owner : 방 주인 회원 번호 (탈퇴한 회원의 방이면 "") , status : "1"(상담 중) / "0" }
        Integer roomId = messageSendDto.getRoomId();
        String roomKey = ROOM_CACHE_PREFIX + roomId;
        Map<Object, Object> roomCache = stringRedisTemplate.opsForHash().entries(roomKey);
        if (roomCache.isEmpty()) {
        //  5-1. 캐시에 없음 → DB 조회 (없는 방이면 null)
        //       SELECT * FROM room WHERE room_id = ?
            RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
            if (roomEntity == null) { return null; }
        //  5-2. Redis 에 60초 보관 (getMemberId( ) 는 LAZY 라도 추가 조회 없이 번호를 꺼낼 수 있음)
            MemberEntity owner = roomEntity.getMemberEntity();
            roomCache = Map.of(
                "owner", owner == null ? "" : owner.getMemberId(),
                "status", roomEntity.getStatus() ? "1" : "0");
            stringRedisTemplate.opsForHash().putAll(roomKey, roomCache);
            stringRedisTemplate.expire(roomKey, CACHE_TTL);
        }

    //  6. 권한 확인 : 관리자는 모든 방 , 회원은 내 방만 (③ 상담 종료의 2번과 같음)
        if (!isAdmin) {
            boolean isOwner = memberId.equals(roomCache.get("owner"));
            if (!isOwner) { return null; }
        }

    //  7. 종료된 방이면 null (상담 중일 때만 대화할 수 있음 — 회원은 [상담 시작] 으로 다시 켜야 함)
        if (!"1".equals(roomCache.get("status"))) { return null; }

    //  8. 메시지 만들기 (DB 에 바로 넣지 않음)
    //     - 번호 : Redis 번호표 — increment( ) = INCR chat:msg:seq (② 상담 시작의 4-5 와 같음)
    //     - 시각 : 지금 (서버가 받은 순간 — 몇 초 뒤 DB 에 들어가도 이 시각 그대로)
    //     - 보낸 사람 종류 : 관리자면 ADMIN , 아니면 MEMBER
    //     - 보낸 사람 이름 : 보낸 그 순간의 이름을 복사 (관리자면 상담원 이름 → 누가 답했는지 남음)
    //     - 방 상태 : 7번에서 상담 중(true)인 것을 확인했으므로 그대로
        SenderType senderType = isAdmin ? SenderType.ADMIN : SenderType.MEMBER;
        MessageDto messageDto = MessageDto.builder()
            .messageId(stringRedisTemplate.opsForValue().increment(SEQ_KEY).intValue())
            .roomId(roomId)
            .senderType(senderType)
            .senderName(senderName)
            .content(content)
            .createdAt(LocalDateTime.now())
            .roomStatus(true)
            .build();
    //     ※ 메시지 남기기 예약 — ⑨ 가 Stream 에 남기고 웹소켓으로 보냄 (보낸 사람도 같은 주소를 구독 중이라 말풍선이 뜸)
        eventPublisher.publishEvent(messageDto);

    //  9. "보낸 사람은 자기 메시지까지 읽은 것으로" 는 여기서 하지 않음 (예전 : 메시지마다 UPDATE room)
    //     → ⑬ 이 DB 로 옮길 때 방마다 한 번에 처리 (보낸 사람 쪽 읽은 시각 = 그 사람이 마지막으로 보낸 메시지 시각)

    //  10. 만든 메시지 반환
        return messageDto;
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

    //  4. 대화에 탈퇴 이력 남기기 (SYSTEM 메시지 — 번호 · 시각은 ② 상담 시작의 4-5 와 같은 방법)
        MessageDto withdrawMessage = MessageDto.builder()
            .messageId(stringRedisTemplate.opsForValue().increment(SEQ_KEY).intValue())
            .roomId(roomEntity.getRoomId())
            .senderType(SenderType.SYSTEM)
            .senderName(SYSTEM_NAME)
            .content(withdrawText)
            .createdAt(LocalDateTime.now())
            .roomStatus(false)
            .build();
    //     ※ 메시지 남기기 예약 — 탈퇴(회원 삭제)까지 모두 성공해야 Stream 에 남고 보내짐 (실패하면 예약도 취소)
        eventPublisher.publishEvent(withdrawMessage);

    //  5. 채팅방에서 회원 연결 끊기 (member_id → NULL)
    //     UPDATE room SET member_id = NULL , status = false ... WHERE room_id = ?
    //     ※ 이 UPDATE 는 MypageService 의 회원 DELETE 보다 먼저 실행됨
    //       (JPA 는 모아 둔 SQL 을 실행할 때 INSERT → UPDATE → DELETE 순서로 실행)
    //     방 캐시는 4번의 SYSTEM 메시지 덕분에 ⑨ 가 지움
        roomEntity.setMemberEntity(null);

    //  6. 회원 캐시 지우기 (탈퇴가 확정된 뒤 — ⑪)
        forgetMember(memberId);
    }


//  =====================================================================
//  ⑨ Stream 에 남기고 웹소켓으로 보내기 — 위의 "※ 메시지 남기기 예약" 이 있으면 Spring 이 자동으로 부름 (직접 부르지 않음)
//
//  - 왜 만든 자리에서 바로 하지 않고 예약하는가
//      이 서비스의 메소드들은 @Transactional 이라 , 메소드가 "끝날 때" DB 저장(방 상태 변경 등)이 확정(commit)됨
//      확정 전에 Stream 에 남기거나 보내면 , 그 뒤에 취소(rollback)됐을 때 일어나지 않은 일이 대화에 남음
//        예) 탈퇴가 중간에 실패했는데 "회원 탈퇴로 상담이 종료되었습니다" 가 기록 · 전송됨
//      → 확정된 "뒤" 에만 Stream 에 남기고 보냄 (취소되면 예약도 같이 취소)
//
//  - Stream 에 남기기 (XADD chat:stream) : DB 대신 여기에 먼저 저장 → ⑬ 이 3초마다 모아서 DB 로 옮김
//    Redis 는 메모리라 DB INSERT 보다 훨씬 빠름 , Stream 은 들어온 순서대로 줄 번호가 붙는 기록장
//    ※ Redis 가 꺼지면 아직 안 옮긴 메시지(최대 3초 분량)를 잃을 수 있음 — 운영에서는 Redis AOF(appendonly yes)를 켜서 디스크에도 기록
//
//  - @TransactionalEventListener : 예약된 값(MessageDto)을 받아서 , 저장이 확정된 "뒤" 에 실행
//    (AuditService 의 @EventListener(ApplicationReadyEvent.class) 처럼 Spring 이 알아서 부르는 메소드)
//  - @Transactional (propagation = NOT_SUPPORTED) : 이 메소드는 트랜잭션 없이 실행
//    (DB 작업 없이 보내기만 함 + Spring 규칙 : 확정 뒤에 실행되는 메소드는 트랜잭션 설정을 따로 표시해야 함)
//
//  - 보내는 곳 2군데
//      /sub/chat/admin         : 관리자 화면 (모든 방 , 상담원 이름 그대로)
//      /sub/chat/room/{roomId} : 그 방의 회원 화면 (상담원 이름은 "고객센터" 로)
//
//  - 방 캐시 지우기 : SYSTEM 메시지는 상담 시작 · 종료 · 탈퇴 때만 생김 = 방 상태 · 주인이 바뀐 때
//    → 저장이 확정된 "뒤" 에 지워야 , 다른 요청이 확정 전의 옛날 값을 다시 캐시에 넣는 일이 없음
//  =====================================================================
    @TransactionalEventListener
    @Transactional (propagation = Propagation.NOT_SUPPORTED)
    public void sendAfterCommit(MessageDto messageDto){

    //  0-1. Stream 에 남기기 (DB 저장 대신 — ⑬ 이 몇 초 뒤 DB 로 옮김)
    //       상담원 이름을 "고객센터" 로 바꾸기(2번) "전" 에 남김 → 기록에는 실제 상담원 이름이 남음
    //       XADD chat:stream * messageId 7 roomId 1 senderType MEMBER ...   ( * : Redis 가 줄 번호를 정함)
        stringRedisTemplate.opsForStream().add(STREAM_KEY, messageDto.toStreamFields());

    //  0-2. 방 상태가 바뀐 메시지(SYSTEM)면 방 캐시 지우기 → 다음 메시지 · 구독은 DB 에서 새 상태를 읽음
        if (messageDto.getSenderType() == SenderType.SYSTEM) {
            stringRedisTemplate.delete(ROOM_CACHE_PREFIX + messageDto.getRoomId());
        }

    //  1. 관리자 화면으로 보내기 (상담원 이름 그대로)
    //     convertAndSend( 보낼 주소 , 내용물 ) : day062 와 같음 — 내용물은 JSON 글자로 바뀌어서 전달됨
        messageTemplate.convertAndSend(ADMIN_TOPIC, messageDto);

    //  2. 회원에게 보낼 때는 상담원(ADMIN) 이름을 "고객센터" 로 바꿈 (① 의 5번과 같은 규칙)
    //     1번에서 이미 JSON 글자로 바뀌어 보내졌기 때문에 , 지금 바꿔도 관리자에게 간 내용은 그대로
        if (messageDto.getSenderType() == SenderType.ADMIN) {
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
//  - 관리자인지 · 활성 회원인지 · 방 주인은 Redis 캐시로 확인 (⑦ 메시지 보내기의 3 · 5번과 같은 방식)
//  - 돌려주는 것 : 구독해도 되면 true
//  =====================================================================
    @Transactional (readOnly = true)
    public boolean canSubscribe(String destination, String memberId){

    //  1. 로그인 안 했거나 , 주소가 없으면 false
        if (memberId == null || destination == null) { return false; }

    //  2. 회원 확인 — Redis 캐시 먼저 , 없으면 DB (⑦ 메시지 보내기의 3번과 같음)
        String memberKey = MEMBER_CACHE_PREFIX + memberId;
        Map<Object, Object> memberCache = stringRedisTemplate.opsForHash().entries(memberKey);
        if (memberCache.isEmpty()) {
        //  SELECT * FROM member WHERE member_id = ?
            MemberEntity memberEntity = memberRepository.findById(memberId).orElse(null);
            if (memberEntity == null) { return false; }
            memberCache = Map.of(
                "status", memberEntity.getStatus() ? "1" : "0",
                "admin", memberEntity.isAdmin() ? "1" : "0",
                "name", memberEntity.getManagerName());
            stringRedisTemplate.opsForHash().putAll(memberKey, memberCache);
            stringRedisTemplate.expire(memberKey, CACHE_TTL);
        }
    //     비활성 회원이면 false
        if (!"1".equals(memberCache.get("status"))) { return false; }

    //  3. 관리자인지 (⑦ 메시지 보내기의 4번과 같음)
        boolean isAdmin = "1".equals(memberCache.get("admin"));

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

    //  7. 관리자는 모든 방을 구독할 수 있음 (방이 있는지는 상관없음 — 없는 방이면 메시지가 오지 않을 뿐)
        if (isAdmin) { return true; }

    //  8. 방 주인 확인 — Redis 캐시 먼저 , 없으면 DB (⑦ 메시지 보내기의 5번과 같음)
        String roomKey = ROOM_CACHE_PREFIX + roomId;
        Map<Object, Object> roomCache = stringRedisTemplate.opsForHash().entries(roomKey);
        if (roomCache.isEmpty()) {
        //  SELECT * FROM room WHERE room_id = ?
            RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
            if (roomEntity == null) { return false; }
            MemberEntity owner = roomEntity.getMemberEntity();
            roomCache = Map.of(
                "owner", owner == null ? "" : owner.getMemberId(),
                "status", roomEntity.getStatus() ? "1" : "0");
            stringRedisTemplate.opsForHash().putAll(roomKey, roomCache);
            stringRedisTemplate.expire(roomKey, CACHE_TTL);
        }

    //  9. 회원은 내 방만 (탈퇴해서 주인이 없는 방이면 owner 가 "" 라서 false)
        return memberId.equals(roomCache.get("owner"));
    }


//  =====================================================================
//  ⑪ 회원 캐시 지우기 — 회원의 권한 · 상태 · 이름이 바뀌거나 탈퇴할 때 부름
//  - 부르는 곳 : AuthorizationService (권한 · 상태 변경) , MypageService (이름 변경) , ⑧ 탈퇴 처리
//  - 저장이 확정(commit)된 "뒤" 에 지움 (⑨ 와 같은 이유)
//      확정 전에 지우면 , 그 사이에 온 메시지가 DB 의 옛날 값을 읽어서 다시 60초 동안 캐시에 넣을 수 있음
//  - TransactionSynchronizationManager : 지금 트랜잭션이 끝난 뒤에 할 일을 등록하는 도구 (MatchingSseService 와 같은 것)
//    트랜잭션 밖에서 불리면(등록할 곳이 없으면) 바로 지움
//  =====================================================================
    public void forgetMember(String memberId){

    //  1. 회원 번호가 없으면 할 일 없음
        if (memberId == null) { return; }
        String memberKey = MEMBER_CACHE_PREFIX + memberId;

    //  2. 트랜잭션 안이면 → 확정된 뒤에 지우기 예약
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stringRedisTemplate.delete(memberKey);
                }
            });
            return;
        }

    //  3. 트랜잭션 밖이면 → 바로 지우기
        stringRedisTemplate.delete(memberKey);
    }


//  =====================================================================
//  ⑫ 서버가 켜질 때 채팅 Redis 준비 (AuditService 의 @EventListener(ApplicationReadyEvent.class) 와 같은 방식)
//  1. 채팅 키 비우기
//    - 왜 필요한가 : ddl-auto = create-drop 이라 서버를 다시 켜면 DB 는 비워지는데 , Redis 는 따로 켜져 있어서 값이 남음
//        → 지난번 "1번 방의 주인 = A" 가 남아 있으면 , 새로 만든 1번 방(주인 B)을 A 가 구독할 수 있음
//        → 지난번 Stream 에 남은 메시지도 이번 DB 에는 없는 방의 것이라 같이 지움
//      ※ 운영(ddl-auto = none — DB 가 그대로 남음)으로 바꾸면 Stream · 번호표는 지우면 안 됨
//        (남은 메시지는 ⑬ 이 이어서 DB 로 옮겨야 함 — 그때는 이 1번에서 chat:stream , chat:msg:seq 를 빼고 지울 것)
//    - "chat:" 으로 시작하는 키만 지움 (로그인 토큰 "RT:" 는 그대로)
//    - SCAN : 키를 조금씩(100개씩) 나눠서 찾음
//        KEYS 는 한 번에 전부 찾느라 그동안 Redis 가 다른 명령(로그인 · 채팅)을 처리하지 못해서 쓰지 않음
//  2. 메시지 번호표를 DB 의 가장 큰 번호부터 이어서 시작 (번호가 겹치면 같은 번호의 메시지가 DB 에 안 들어감)
//  3. Stream 과 소비자 그룹 만들기 (⑬ 이 "어디까지 옮겼는지" 를 Redis 가 기억해 주는 장치)
//  =====================================================================
    @EventListener (ApplicationReadyEvent.class)
    @Transactional (propagation = Propagation.NOT_SUPPORTED)
    public void prepareChatRedis(){

    //  1. "chat:" 키 비우기 (캐시 · Stream · 번호표 · 속도 제한)
        ScanOptions scanOptions = ScanOptions.scanOptions().match("chat:*").count(100).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            cursor.forEachRemaining((key) -> stringRedisTemplate.delete(key));
        }

    //  2. 번호표 = 지금 DB 의 가장 큰 메시지 번호 (메시지가 없으면 0 → 다음 메시지는 1번)
    //     SET chat:msg:seq 0
        stringRedisTemplate.opsForValue().set(SEQ_KEY, String.valueOf(messageRepository.findMaxMessageId()));

    //  3. Stream + 소비자 그룹 만들기
    //     XGROUP CREATE chat:stream chat-flush 0 MKSTREAM
    //     - 0 : Stream 의 처음부터 읽기 , MKSTREAM : Stream 이 아직 없으면 같이 만들기 (true)
    //     - StringRedisTemplate 에는 MKSTREAM 을 붙이는 메소드가 없어서 , execute 로 Redis 연결을 직접 받아서 명령을 보냄
    //       (RedisCallback : "이 연결로 이 일을 해 줘" 를 람다로 넘김 , Redis 명령은 글자가 아니라 byte 로 보내야 해서 getBytes)
        stringRedisTemplate.execute( (RedisCallback<String>) (connection) ->
            connection.streamCommands().xGroupCreate(STREAM_KEY.getBytes(StandardCharsets.UTF_8), FLUSH_GROUP, ReadOffset.from("0"), true) );

    //  4. 준비 끝 → 이제부터 ⑬ 이 동작
        streamReady = true;
    }


//  =====================================================================
//  ⑬ Stream → DB 옮기기 (배치 저장) — 3초마다 Spring 이 자동으로 부름 (@Scheduled , MainProjectApplication 의 @EnableScheduling)
//
//  - 왜 모아서 넣는가 : DB 는 한 번 다녀올 때마다(디스크 기록 · 확정 · 잠금) 드는 비용이 커서 ,
//    메시지 100개를 INSERT 100번 하는 것보다 INSERT 1번에 100줄을 넣는 쪽이 훨씬 빠름
//  - 왜 3초인가 : 상담이 끝날 때까지 모으면 서버 · Redis 가 꺼졌을 때 대화 전체를 잃을 수 있음
//    → 짧게 자주 옮겨서 잃을 수 있는 양을 최대 몇 초로 줄임
//  - fixedDelay = 3000 : 이전 실행이 "끝난 뒤" 3초 쉬고 다시 (이전 실행이 길어져도 두 번이 겹쳐 돌지 않음)
//
//  - 빠짐없이 · 두 번 없이 옮기는 방법
//    1. 소비자 그룹(chat-flush)으로 읽으면 Redis 가 "누가 어디까지 받아 갔는지" 를 기억함
//    2. DB 저장이 확정된 "뒤" 에 확인 도장(XACK)을 찍고 Stream 에서 지움(XDEL)
//       → 저장 중에 서버가 꺼지면 도장이 없어서 , 다음 실행 때 같은 메시지를 다시 받아 감 (2-1)
//    3. 다시 넣을 때 이미 들어간 번호는 INSERT IGNORE 로 건너뜀 (message_id 가 PK 라서 같은 번호는 두 번 못 들어감)
//
//  - 보낸 사람 쪽 "읽은 시각" 도 여기서 방마다 한 번에 바꿈 (예전 : 메시지마다 UPDATE room — ⑦ 의 9번)
//  =====================================================================
    @Scheduled (fixedDelay = 3000)
    public void flushMessages(){

    //  1. ⑫ 준비가 끝나기 전이면 건너뜀 (소비자 그룹이 없으면 읽기 오류)
        if (!streamReady) { return; }

    //  2. 옮길 메시지 읽기 (최대 500개)
    //     XREADGROUP GROUP chat-flush server-1 COUNT 500 STREAMS chat:stream 0   (2-1)
    //     XREADGROUP GROUP chat-flush server-1 COUNT 500 STREAMS chat:stream >   (2-2)
    //     2-1. 지난번에 받아 갔지만 확인 도장(XACK)이 없는 것 먼저 — ReadOffset.from("0")
    //          (저장 도중 서버가 꺼졌거나 DB 오류로 취소된 것)
    //     2-2. 없으면 새로 들어온 것 — ReadOffset.lastConsumed( ) = ">" (아직 아무도 안 받아 간 것)
        Consumer consumer = Consumer.from(FLUSH_GROUP, FLUSH_CONSUMER);
        StreamReadOptions readOptions = StreamReadOptions.empty().count(FLUSH_BATCH_SIZE);
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
            .read(consumer, readOptions, StreamOffset.create(STREAM_KEY, ReadOffset.from("0")));
        if (records == null || records.isEmpty()) {
            records = stringRedisTemplate.opsForStream()
                .read(consumer, readOptions, StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));
        }
        if (records == null || records.isEmpty()) { return; }   // 옮길 것이 없으면 끝 (DB 에 가지 않음)

    //  3. Stream 한 줄 → DTO (MessageDto.fromStream)
        List<MessageDto> messageDtos = records.stream().map( (record) -> MessageDto.fromStream(record.getValue()) ).toList();

    //  4. 메시지 전부를 INSERT 1번으로 넣기
    //     INSERT IGNORE INTO message (...) VALUES (? , ...) , (? , ...) , (? , ...)   ← 메시지 수만큼 ( ) 를 이어 붙임
    //     - Collections.nCopies( n , 글자 ) : 같은 글자 n 개짜리 목록 → String.join 으로 ", " 를 사이에 넣어 한 줄로
    //     - 값은 ? 자리에 순서대로 들어감 (글자를 SQL 에 직접 붙이지 않아서 SQL 인젝션이 안 됨)
    //     - IGNORE : 이미 있는 번호(다시 옮기는 경우)는 오류 없이 건너뜀
    //       ※ 그 대신 다른 오류도 경고로 바꾸고 건너뛰므로 , 길이 등은 ⑦ 에서 미리 검사해 둠 (내용 1000자)
    //     - 엔티티(JPA save)로 넣지 않는 이유 : save 는 한 개씩 INSERT 하고 , 번호를 직접 정한 엔티티는 먼저 SELECT 까지 함
        String rows = String.join(", ", Collections.nCopies(messageDtos.size(), "(?, ?, ?, ?, ?, ?)"));
        List<Object> values = new ArrayList<>();
        messageDtos.forEach( (messageDto) -> {
            values.add(messageDto.getMessageId());
            values.add(messageDto.getRoomId());
            values.add(messageDto.getSenderType().name());
            values.add(messageDto.getSenderName());
            values.add(messageDto.getContent());
            values.add(messageDto.getCreatedAt());
        } );
        int insertedCount = jdbcTemplate.update(
            "INSERT IGNORE INTO message (message_id, room_id, sender_type, sender_name, content, created_at) VALUES " + rows,
            values.toArray());

    //  5. 보낸 사람 쪽 읽은 시각 (방마다 한 번)
    //     - 회원이 보낸 메시지가 있으면 member_read_at , 상담원이 보낸 메시지가 있으면 admin_read_at 을 그 사람의 마지막 메시지 시각으로
    //       (내가 답을 보냈다면 그 앞에 온 상대 메시지는 이미 본 것)
    //     - 자동 인사(이름이 "고객센터" 인 ADMIN 메시지)는 사람이 보낸 게 아니라서 제외
    //     - merge( 키 , 값 , 규칙 ) : 키가 없으면 넣고 , 있으면 규칙으로 고름 → 더 늦은 시각을 남김
    //     - GREATEST(COALESCE(읽은 시각 , ?) , ?) : 이미 저장된 읽은 시각이 더 늦으면 그대로 둠
    //       (그 사이 ⑥ 읽음 처리로 "지금" 이 들어갔을 수 있어서 , 옛날 시각으로 되돌리지 않게)
    //     UPDATE room SET member_read_at = ... , updated_at = NOW() WHERE room_id = ?
        Map<Integer, LocalDateTime> memberSentAt = new HashMap<>();
        Map<Integer, LocalDateTime> adminSentAt = new HashMap<>();
        messageDtos.forEach( (messageDto) -> {
            if (messageDto.getSenderType() == SenderType.MEMBER) {
                memberSentAt.merge(messageDto.getRoomId(), messageDto.getCreatedAt(), (a, b) -> a.isAfter(b) ? a : b);
            }
            if (messageDto.getSenderType() == SenderType.ADMIN && !CENTER_NAME.equals(messageDto.getSenderName())) {
                adminSentAt.merge(messageDto.getRoomId(), messageDto.getCreatedAt(), (a, b) -> a.isAfter(b) ? a : b);
            }
        } );
        memberSentAt.forEach( (roomId, sentAt) -> jdbcTemplate.update(
            "UPDATE room SET member_read_at = GREATEST(COALESCE(member_read_at, ?), ?), updated_at = NOW() WHERE room_id = ?",
            sentAt, sentAt, roomId) );
        adminSentAt.forEach( (roomId, sentAt) -> jdbcTemplate.update(
            "UPDATE room SET admin_read_at = GREATEST(COALESCE(admin_read_at, ?), ?), updated_at = NOW() WHERE room_id = ?",
            sentAt, sentAt, roomId) );

    //  6. DB 저장이 확정된 "뒤" 에 확인 도장(XACK) + Stream 에서 지우기(XDEL) — ⑪ 과 같은 "확정 뒤에 할 일" 등록
    //     확정 전에 도장을 찍으면 , 저장이 취소됐을 때 그 메시지를 다시 받을 방법이 없음 (영영 사라짐)
    //     RecordId : Stream 의 줄 번호 (예: 1760500000000-0)
        RecordId[] recordIds = records.stream().map( (record) -> record.getId() ).toArray(RecordId[]::new);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, FLUSH_GROUP, recordIds);
                stringRedisTemplate.opsForStream().delete(STREAM_KEY, recordIds);
            }
        });

    //  7. 콘솔에 기록 (JdbcTemplate 으로 실행한 SQL 은 spring.jpa.show-sql 에 안 찍혀서 , 몇 개를 몇 번에 넣었는지 직접 남김)
        log.info("[채팅 배치 저장] 메시지 {}개 → INSERT 1번 (새로 들어간 줄 {}개) , 읽은 시각 UPDATE {}번",
            messageDtos.size(), insertedCount, memberSentAt.size() + adminSentAt.size());
    }

}
