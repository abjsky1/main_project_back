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
public class Match_logEntity {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer match_id;

    @Column ( nullable = false)
    private Integer company_member_id;
    
    
    @Column ( nullable = false)
    private Integer logistics_member_id;

    @Builder.Default
    @Column ( nullable = false)
    private Integer matched_country_count = 0;

    @Builder.Default
    @Column ( nullable = false)
    private Integer matched_hscode_count = 0;

    
    @Column ( nullable = false)
    private Integer country_trade_id;

    
    @Column ( nullable = false)
    private LocalDateTime match_status_update_at = LocalDateTime.now();

    @Column ( nullable = false)
    private Integer match_status;
    
    @ManyToOne  ( fetch = FetchType.LAZY)
    @JoinColumn  ( name = "member_id") 
    private MemberEntity company_memberEntity;

    @ManyToOne ( fetch = FetchType.LAZY)
    @JoinColumn  ( name = "member_id") 
    private MemberEntity logistics_memberEntity;

    @ManyToOne ( fetch = FetchType.LAZY)
    @JoinColumn  ( name = "country_trade_id") 
    private Trade_statisticsEntity Trade_statisticsEntity;


}
