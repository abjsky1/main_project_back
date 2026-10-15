package main_project.member.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import main_project.global.util.EncryptedStringConverter;
import main_project.condition.entity.Cscore1Entity;
import main_project.condition.entity.Lscore1Entity;
import main_project.global.entity.BaseTime;


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

//  ---------- 개인정보 암호화 (이메일 · 담당자 이름 · 사업자번호 · 전화번호) ----------
//  - @Convert : DB에 저장할 때 AES-256-GCM 암호화, 읽을 때 자동 복호화 → 자바 코드에서는 평문으로 보임
//  - DB 저장 모양 : "v1:" + Base64( IV 12바이트 + 암호문 + 태그 16바이트 )
//  - 컬럼 길이 = 3 + ceil( (28 + 평문 바이트) / 3 ) * 4  (평문은 한 글자 최대 4바이트로 계산)
//      이메일 100자 → 575 → 600 , 이름 50자 → 307 → 320 , 사업자번호 12자 → 107 → 120 , 전화번호 20자 → 147 → 160
//  - 암호문은 같은 값이라도 매번 달라서 unique · 검색이 안 됨 → 이메일 · 사업자번호는 아래 *_hash 칸으로 중복확인 · 검색

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "user_email", nullable = false, length = 600)
    private String userEmail;

    @Column(name = "user_password", nullable = false, length = 255)
    private String userPassword;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "manager_name", nullable = false, length = 320)
    private String managerName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "business_reg_no", nullable = false, length = 120)
    private String businessRegNo;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "user_phone", nullable = false, length = 160)
    private String userPhone;

//  ---------- 검색 · 중복확인용 HMAC-SHA-256 값 (16진수 64글자) ----------
//  - 정규화(PiiNormalizer)한 값을 HmacUtil로 만든 값. 같은 입력이면 항상 같은 값 → WHERE · unique 가능
//  - nullable : 예전에는 평문 샘플 회원을 서버 시작 후 암호화하면서 채웠음. 지금은 샘플 SQL에 값이 들어있음 (not null 전환은 보류)
//  - 이름 · 전화번호는 검색값을 만들지 않음 (이름은 동명이인이 있어 unique 불가)

    @Column(name = "user_email_hash", length = 64, unique = true)
    private String userEmailHash;

    @Column(name = "business_reg_no_hash", length = 64, unique = true)
    private String businessRegNoHash;

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

//  ---------- 이 회원이 관리자인지 (role 2 = 관리자) ----------
//  - "관리자 = role 2" 규칙을 여기 한 곳에만 둠 → 권한 확인 · 마이페이지 · 감사 로그 · 채팅이 모두 이 메소드를 씀
//    예) if (memberEntity.isAdmin()) { ... }
//  - true / false 를 돌려주는 메소드라 이름을 isXxx 로 (Java 관례 — Lombok 이 boolean 칸에 만드는 getter 와 같은 모양)
//  - roleEntity 는 LAZY 지만 getRoleId( ) 처럼 번호만 읽으면 role 테이블을 추가로 조회하지 않음
//  - DB 칸이 아니라 메소드라서 테이블에는 아무것도 생기지 않음
    public boolean isAdmin(){
        return roleEntity.getRoleId() == RoleEntity.ADMIN_ROLE_ID;
    }
}
