package main_project.chat.entity;

//  =====================================================================
//  채팅 메시지의 보낸 사람 종류 (message.sender_type) — enum(열거형)
//
//  - enum : "정해진 몇 가지 값만 들어갈 수 있는 타입" (요일 7개 , 계절 4개 처럼)
//      → 보낸 사람 종류는 아래 3가지뿐이라 enum 으로 묶음
//  - 왜 글자(String) 대신 enum 인가
//      글자 : "ADMN" 같은 오타도 컴파일이 됨 → 실행해 봐야 틀린 걸 알 수 있음 , 아무 글자나 넣을 수 있음
//      enum : SenderType.ADMN 처럼 오타를 내면 바로 빨간 줄 , 이 3가지 말고는 넣을 수 없음 (type safe)
//  - 쓰는 법 (같은 상수는 같은 객체라서 == 로 비교)
//      .senderType(SenderType.ADMIN)
//      if (messageDto.getSenderType() == SenderType.ADMIN) { ... }
//
//  - DB 에는 이름 글자 그대로 저장 ("MEMBER" / "ADMIN" / "SYSTEM")
//      → MessageEntity 의 @Enumerated(EnumType.STRING) 이 name( ) 값을 저장하고 , 읽을 때는 valueOf( ) 로 다시 enum 으로 바꿈
//  - API 응답(JSON)에도 이름 글자 그대로 나감 → 프론트(chatApi.ts 의 SenderType)는 바꿀 필요 없음
//
//  ⚠️ 상수 이름을 바꾸면 이미 저장된 DB 값("ADMIN" 등)을 읽을 수 없게 되니 , 이름은 바꾸지 말 것
//     (새 종류를 추가하는 건 괜찮음)
//
//  role 은 DB 에 role 테이블이 따로 있어서 상수(RoleEntity.ADMIN_ROLE_ID)로 두었지만 ,
//  보낸 사람 종류는 테이블 없이 코드에만 있는 목록이라 enum 이 이 목록의 "원본" 이 됨
//  =====================================================================
public enum SenderType {

    MEMBER,   // 회원이 보낸 메시지
    ADMIN,    // 고객센터(상담원)가 보낸 메시지 (상담 시작 인사 포함)
    SYSTEM    // "상담이 시작되었습니다" 같은 자동 안내

}
