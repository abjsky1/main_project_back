package main_project.global.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;

//  생성 시간(BaseCreatedTime 에서 물려받음) + 수정 시간(updated_at)
//  - 수정 기능이 있는 테이블용 : member · room · matching
//  - 수정이 없는 테이블은 BaseCreatedTime 만 상속 (updated_at 칸을 만들지 않음)
@Getter
@NoArgsConstructor
@MappedSuperclass
public class BaseTime extends BaseCreatedTime {

    @LastModifiedDate
    private LocalDateTime updatedAt;

}
