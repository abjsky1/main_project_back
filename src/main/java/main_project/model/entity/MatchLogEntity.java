package main_project.model.entity;

import jakarta.persistence.ManyToOne;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "matchLog")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class MatchLogEntity extends BaseTime {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer matchLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companyMemberId", nullable = false)
    private MemberEntity companyMemberEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logisticsMemberId", nullable = false)
    private MemberEntity logisticsMemberEntity;

    @Builder.Default
    @Column ( nullable = false)
    private Integer matchedCountryCount = 0;

    @Builder.Default
    @Column ( nullable = false)
    private Integer matchedHscodeCount = 0;

    // @ManyToOne ( fetch = FetchType.LAZY)
    // @JoinColumn  ( name = "countryTradeId" , nullable = false ) 
    // private TradeStatisticsEntity TradeStatisticsEntity;

    @Column ( nullable = false)
    private Integer matchStatus;
    


}
