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
@Table (name = "trade_type")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class Trade_typeEntity {

    @Id 
    @GeneratedValue ( strategy = GenerationType.IDENTITY )
    private Integer trade_id;

    @Column ( nullable = false )
    private Integer member_id;
    
    @Column  ( length = 15 , nullable = false)
    private Integer hs_code;

    @Column  ( nullable = false )
    private Integer country_id;

    @Column  ( nullable =  false )
    private boolean matching_agree = false;

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn ( name = "member_id" )
    private MemberEntity memberEntity;


}
