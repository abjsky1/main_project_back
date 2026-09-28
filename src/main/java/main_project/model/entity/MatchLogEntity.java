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

@Entity
@Table(name = "match_log")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MatchLogEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_log_id")
    private Integer matchLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( name = "company_member_id", nullable = false )
    private MemberEntity companyMemberEntity;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( name = "logistics_member_id", nullable = false )
    private MemberEntity logisticsMemberEntity;

    @Builder.Default
    @Column( name = "matched_country_count", nullable = false )
    private Integer matchedCountryCount = 0;

    @Builder.Default
    @Column( name = "matched_hscode_count", nullable = false )
    private Integer matchedHscodeCount = 0;

    @Column( name = "match_status", nullable = false )
    private Integer matchStatus;

}