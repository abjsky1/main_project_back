package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.Cscore1Entity;
import main_project.model.entity.MemberEntity;

@AllArgsConstructor 
@NoArgsConstructor 
@Builder
@Data 
public class Cscore1Dto {

    private Integer cscore1Id;

    private String memberId;

    private Integer countryId;

    private String hsCode;

    private Boolean tradeType;

    private Boolean transportType;

    private Integer departure;

    private Integer arrival;

    private Boolean matchingAgree;

    public Cscore1Entity toEntity(MemberEntity memberEntity ){

        return Cscore1Entity.builder()
                .memberEntity(memberEntity)
                .countryId(countryId)
                .hsCode(hsCode)
                .tradeType(tradeType)
                .transportType(transportType)
                .departure(departure)
                .arrival(arrival)
                .matchingAgree(matchingAgree)
                .build();
        }

    public static Cscore1Dto from( Cscore1Entity entity ){

        return Cscore1Dto.builder()
                .cscore1Id(entity.getCscore1Id())
                .memberId(entity.getMemberEntity().getMemberId())
                .countryId(entity.getCountryId())
                .hsCode(entity.getHsCode())
                .tradeType(entity.getTradeType())
                .transportType(entity.getTransportType())
                .departure(entity.getDeparture())
                .arrival(entity.getArrival())
                .matchingAgree(entity.getMatchingAgree())
                .build();
    }
    
    

}
