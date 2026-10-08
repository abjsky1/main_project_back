package main_project.model.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Entity
@Table(name = "member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEntity extends BaseTime {

    @Id
    @Column(name = "member_id")
    private String  memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signup_id", nullable = false)
    private SignupEntity signupEntity;

    @Column(name = "user_email", nullable = false, length = 100, unique = true)
    private String userEmail;

    @Column(name = "user_password", nullable = false, length = 255)
    private String userPassword;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName;

    @Column(name = "business_reg_no", nullable = false, length = 12, unique = true)
    private String businessRegNo;

    @Column(name = "user_phone", nullable = false, length = 20)
    private String userPhone;

    @Column(name = "company_address", nullable = false, length = 300)
    private String companyAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity roleEntity;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private Boolean status = true;


//  ---------- 회원 탈퇴(회원 삭제) 할 때 같이 지울 자식 데이터 ----------
//  springweb day043 StudentEntity 와 같은 방식 : 부모(회원) 쪽에 자식 목록을 두고 cascade 를 붙임
//  - mappedBy = "memberEntity" : 자식 엔티티의 memberEntity 칸이 FK(member_id) 를 가지고 있다는 뜻
//                                (DB 칸이 새로 생기지 않음 — 자식 쪽 FK 를 그대로 사용)
//  - cascade = CascadeType.REMOVE : 부모(회원)가 *삭제* 되면 자식도 같이 *삭제*
//                                   (ALL 이 아니라 REMOVE 만 — 저장 · 수정은 지금처럼 각 Service 가 직접 함)
//  - @ToString.Exclude , @EqualsAndHashCode.Exclude : @Data 가 만드는 toString / equals 에서 빼기
//                                   (회원 → 조건 → 회원 → 조건 ... 끝없이 서로를 부르는 것 방지)
//  - @Builder.Default : builder 로 회원을 만들 때도 빈 목록으로 시작 (null 방지)
//  ※ 감사 로그(audit)는 여기에 없음 → 회원을 지워도 로그는 지우지 않음
//    (탈퇴 전에 MypageService 가 로그를 "탈퇴 회원" 공용 계정으로 옮김)

    // 화주 매칭 조건 (cscore1) — Cscore1Entity 쪽에서 다시 2·3번 조건과 매칭 결과까지 같이 삭제
    @OneToMany(mappedBy = "memberEntity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Cscore1Entity> cscore1Entities = new ArrayList<>();

    // 물류 매칭 조건 (lscore1) — Lscore1Entity 쪽에서 다시 2·3번 조건과 매칭 결과까지 같이 삭제
    @OneToMany(mappedBy = "memberEntity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Lscore1Entity> lscore1Entities = new ArrayList<>();

    // 관심 국가 (interest)
    @OneToMany(mappedBy = "memberEntity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<InterestEntity> interestEntities = new ArrayList<>();
}
