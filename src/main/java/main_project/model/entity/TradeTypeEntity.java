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
@Table (name = "tradeType")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class TradeTypeEntity extends BaseTime {

    @Id 
    @GeneratedValue ( strategy = GenerationType.IDENTITY )
    private Integer tradeId;

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn ( name = "memberId" , nullable = false )
    private MemberEntity memberEntity;
    
    @Column  ( length = 15 , nullable = false)
    private String hsCode;

    @Column  ( nullable = false )
    private Integer countryId;

    @Builder.Default
    @Column  ( nullable =  false )
    private boolean matchingAgree = false;

}
