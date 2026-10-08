package main_project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main_project.model.entity.InterestEntity;
import main_project.model.entity.MemberEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestDto {

//  관심 국가 번호 (삭제할 때 사용)
    private Integer interestId;

//  회원 번호
    private String memberId;

//  국가 번호 (country.csv 의 country_id)
    private Integer countryId;

//  국가 이름 (DB 에는 없음 → InterestService 가 country.csv 를 보고 채워서 화면에 보냄)
    private String countryName;


//  DTO → Entity (저장할 때)
//  회원 엔티티는 서비스가 DB 에서 찾아서 넘겨줌 (Cscore1Dto.toEntity(memberEntity) 와 같은 방식)
    public InterestEntity toEntity(MemberEntity memberEntity) {
        return InterestEntity.builder()
                .memberEntity(memberEntity)
                .countryId(countryId)
                .build();
    }

//  Entity → DTO (조회할 때)
//  국가 이름은 엔티티 안에 없으니까 매개변수로 따로 받음 (AuthorizationDto.from 과 같은 방식)
    public static InterestDto from(InterestEntity interestEntity, String countryName) {
        return InterestDto.builder()
                .interestId(interestEntity.getInterestId())
                .memberId(interestEntity.getMemberEntity().getMemberId())
                .countryId(interestEntity.getCountryId())
                .countryName(countryName)
                .build();
    }

}
