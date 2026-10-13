package main_project.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "role")
@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity extends BaseTime {

//  role 테이블 번호 (MainDBSampleData.sql : 1 = ROLE_USER , 2 = ROLE_ADMIN)
//  - 여러 곳에서 쓰는 번호라 public 으로 여기 한 곳에만 둠 (프론트 App.tsx 의 ADMIN_ROLE_ID 도 같은 값)
//  - static : 객체(행) 하나하나가 아니라 클래스에 하나만 있는 값 → DB 칸이 생기지 않음
//  - "관리자인지" 확인은 이 번호를 직접 비교하지 말고 MemberEntity.isAdmin( ) 을 쓸 것
    public static final int USER_ROLE_ID = 1;    // 일반 회원 (회원가입하면 이 권한)
    public static final int ADMIN_ROLE_ID = 2;   // 관리자

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", nullable = false, length = 20, unique = true)
    private String roleName;

    @Column(name = "role_description", nullable = false, length = 100)
    private String roleDescription;
}
