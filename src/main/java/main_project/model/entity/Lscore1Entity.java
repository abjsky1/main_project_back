package main_project.model.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "lscore1")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lscore1Entity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lscore1_id")
    private Integer lscore1Id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "hs_code", nullable = false, length = 15)
    private String hsCode;

    @Column(name = "trade_type", nullable = false)
    private Boolean tradeType;

    @Column(name = "transport_type", nullable = false)
    private Boolean transportType;

    @Column(name = "departure", nullable = false)
    private Integer departure;

    @Column(name = "arrival", nullable = false)
    private Integer arrival;

    @Builder.Default
    @Column(name = "matching_agree" , nullable = false)
    private Boolean matchingAgree = false;

    @Builder.Default
    @Column(name = "experience_count" , nullable = false)
    private Integer experienceCount = 0;

    @Builder.Default
    @Column(name = "regular_route" , nullable = false)
    private Boolean regularRoute = true;

    @Builder.Default
    @Column(name = "direct_route" , nullable = false)
    private Boolean directRoute = true;


//  ---------- 회원 탈퇴로 이 조건이 삭제될 때 같이 지울 자식 데이터 ----------
//  (MemberEntity 의 lscore1Entities 에서 이어지는 cascade — 회원 → 조건 1번 → 아래 3가지)
//  mappedBy = "lscore1Entity" : 자식 엔티티의 lscore1Entity 칸이 FK(lscore1_id) 를 가지고 있다는 뜻 (DB 칸 추가 없음)
//  cascade = CascadeType.REMOVE : 이 조건이 삭제되면 자식도 같이 삭제
//  ※ 조건 삭제 API(DELETE /api/lscore/{id})는 지금처럼 Service 가 2·3번을 먼저 지우고 , 매칭 결과가 있으면 삭제를 막음 (동작 변화 없음)

    // 2번 조건 (물량 · 일정) — 조건 1개에 1개
    @OneToOne(mappedBy = "lscore1Entity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Lscore2Entity lscore2Entity;

    // 3번 조건 (화물 특성) — 조건 1개에 1개
    @OneToOne(mappedBy = "lscore1Entity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Lscore3Entity lscore3Entity;

    // 이 조건으로 만들어진 매칭 결과 (matching)
    @OneToMany(mappedBy = "lscore1Entity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<MatchingEntity> matchingEntities = new ArrayList<>();
}
