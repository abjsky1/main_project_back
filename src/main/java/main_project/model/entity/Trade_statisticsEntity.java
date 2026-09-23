package main_project.model.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "sample")
@AllArgsConstructor 
@NoArgsConstructor
@Data 
@Builder
public class Trade_statisticsEntity {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer country_trade_id;

    @Column ( name = "trade_balance" , nullable = false , precision = 18 , scale = 2 )
    private BigDecimal trade_balance;

    @Column ( name = "export_amount" , nullable = false , precision = 18 , scale = 2 )
    private BigDecimal export_amount;

    @Column ( name = "export_weight" , nullable = true , precision = 18 , scale = 3 )
    private BigDecimal export_weight;

    @Column ( name = "hs_code" , nullable = false , length = 15 )
    private String hs_code;

    @Column ( name = "import_amount" , nullable = false , precision = 18 , scale = 2 )
    private BigDecimal import_amount;

    @Column ( name = "import_weight" , nullable = true , precision = 18 , scale = 3 )
    private BigDecimal import_weight;

    @Column ( name = "country_id" , nullable = false )
    private Integer country_id;

}
