package main_project.model.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "search_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistoryEntity {

    // 검색 기록 번호: 정수이므로 length를 지정하지 않음
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    // 검색한 회원: 기존 회원 테이블의 ID와 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MemberEntity memberEntity;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "hscode", nullable = false, length = 10)
    private String hscode;

    @Column(name = "start_date", nullable = false, length = 10)
    private String startdate;

    @Column(name = "end_date", nullable = false, length = 10)
    private String enddate;

    // 저장할 검색 기록 객체 생성
    public SearchHistoryEntity(
            MemberEntity memberEntity,
            String country,
            String hscode,
            String startdate,
            String enddate
    ) {
        this.memberEntity = memberEntity;
        this.country = country;
        this.hscode = hscode;
        this.startdate = startdate;
        this.enddate = enddate;
    }
}