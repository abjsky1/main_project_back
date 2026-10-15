package main_project.global.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;

//  =====================================================================
//  생성 시간(created_at)만 자동 기록하는 부모 클래스
//
//  - 수정 기능이 없는 테이블용 : 한 번 저장하면 바뀌지 않으므로 updated_at 칸이 필요 없음
//      action · role · signup (기준 정보 — 샘플 SQL 로만 넣음)
//      cscore1 · lscore1     (매칭 조건 — 등록 · 삭제만 있고 수정 API 없음)
//  - 수정되는 테이블(member · room · matching)은 이 클래스를 이어받은 BaseTime 을 상속 → updated_at 까지
//  - 감사 로그(audit)는 이 클래스 없이 엔티티 안에 created_at 을 직접 둠 (같은 방식)
//  =====================================================================
@Getter
@NoArgsConstructor
@MappedSuperclass
@EntityListeners (AuditingEntityListener.class)
public class BaseCreatedTime {

    //  updatable = false : 한 번 저장된 뒤에는 UPDATE 문에 포함되지 않음 → 생성 시간이 바뀔 일 없음
    @CreatedDate
    @Column (name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
