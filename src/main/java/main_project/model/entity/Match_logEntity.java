package main_project.model.entity;

import java.time.LocalDateTime;

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
@Table (name = "match_log")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class Match_logEntity extends BaseTime {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer match_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_member_id", nullable = false)
    private MemberEntity company_memberEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_member_id", nullable = false)
    private MemberEntity logistics_memberEntity;

    @Builder.Default
    @Column ( nullable = false)
    private Integer matched_country_count = 0;

    @Builder.Default
    @Column ( nullable = false)
    private Integer matched_hscode_count = 0;

    
    @ManyToOne ( fetch = FetchType.LAZY)
    @JoinColumn  ( name = "country_trade_id" , nullable = false ) 
    private Trade_statisticsEntity Trade_statisticsEntity;
    
    // @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    // private LocalDateTime match_status_update_at;

    @Column ( nullable = false)
    private Integer match_status;
    


}
