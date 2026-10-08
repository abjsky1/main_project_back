package main_project.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//  관심 국가 (프론트 마이페이지에서 고르고 → 맞춤 인사이트 화면에 국가별로 표시)
//  - 회원 1명당 최대 3개 → 3개가 찬 상태에서 추가하면 가장 먼저 등록한 국가가 빠짐 (InterestService 의 밀어내기)
//  - 국가 이름은 저장하지 않고 country.csv 의 번호(country_id)만 저장 → 화면에 보낼 때 InterestService 가 이름으로 바꿈
@Entity
@Table(name = "interest")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestEntity extends BaseTime {

    // 관심 국가 번호 (PK , 자동 증가) → 번호가 작을수록 먼저 등록한 국가
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Integer interestId;

    // 어떤 회원의 관심 국가인지 (FK → member 테이블의 member_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity memberEntity;

    // 국가 번호 (static/country/country.csv 의 country_id , 예: 1233 = 미국)
    @Column(name = "country_id", nullable = false)
    private Integer countryId;

}
