package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.Lscore1Entity;
import main_project.model.entity.MemberEntity;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Lscore1Dto {

    private Integer lscore1Id;

    private Integer memberId;

    private Integer countryId;

    private String hsCode;

    private Boolean tradeType;

    private Boolean transportType;

    private Integer departure;

    private Integer arrival;

    private Boolean matchingAgree;

    private Integer experienceCount;

    private Boolean regularRoute;

    private Boolean directRoute;


    // DTO -> Entity
    public Lscore1Entity toEntity(MemberEntity memberEntity) {

        return Lscore1Entity.builder()
                .memberEntity(memberEntity)
                .countryId(countryId)
                .hsCode(hsCode)
                .tradeType(tradeType)
                .transportType(transportType)
                .departure(departure)
                .arrival(arrival)
                .matchingAgree(matchingAgree)
                .experienceCount(experienceCount)
                .regularRoute(regularRoute)
                .directRoute(directRoute)
                .build();

    }


    // Entity -> DTO
    public static Lscore1Dto from(Lscore1Entity entity) {

        return Lscore1Dto.builder()
                .lscore1Id(entity.getLscore1Id())
                .memberId(entity.getMemberEntity().getMemberId())
                .countryId(entity.getCountryId())
                .hsCode(entity.getHsCode())
                .tradeType(entity.getTradeType())
                .transportType(entity.getTransportType())
                .departure(entity.getDeparture())
                .arrival(entity.getArrival())
                .matchingAgree(entity.getMatchingAgree())
                .experienceCount(entity.getExperienceCount())
                .regularRoute(entity.getRegularRoute())
                .directRoute(entity.getDirectRoute())
                .build();
                
    }

}