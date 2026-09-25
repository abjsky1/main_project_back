// package main_project.model.entity;

// import java.math.BigDecimal;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// @Entity 
// @Table (name = "tradeStatistics")
// @AllArgsConstructor 
// @NoArgsConstructor
// @Data 
// @Builder
// public class TradeStatisticsEntity extends BaseTime {

//     @Id 
//     @GeneratedValue (strategy = GenerationType.IDENTITY)
//     private Integer countryTradeId;

//     @Column ( name = "tradeBalance" , nullable = false , precision = 18 , scale = 2 )
//     private BigDecimal tradeBalance;

//     @Column ( name = "exportAmount" , nullable = false , precision = 18 , scale = 2 )
//     private BigDecimal exportAmount;

//     @Column ( name = "exportWeight" , nullable = true , precision = 18 , scale = 3 )
//     private BigDecimal exportWeight;

//     @Column ( name = "hsCode" , nullable = false , length = 15 )
//     private String hsCode;

//     @Column ( name = "importAmount" , nullable = false , precision = 18 , scale = 2 )
//     private BigDecimal importAmount;

//     @Column ( name = "importWeight" , nullable = true , precision = 18 , scale = 3 )
//     private BigDecimal importWeight;

//     @Column ( name = "countryId" , nullable = false )
//     private Integer countryId;

// }
