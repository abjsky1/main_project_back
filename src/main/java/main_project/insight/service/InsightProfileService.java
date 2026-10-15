package main_project.insight.service;

import lombok.RequiredArgsConstructor;

import main_project.member.dto.MemberDto;
import main_project.insight.dto.InsightDto.Condition;
import main_project.insight.dto.InsightDto.Country;
import main_project.insight.dto.InsightDto.Profile;
import main_project.global.util.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.TemporalAccessor;
import java.util.*;
import main_project.auth.service.LoginService;
import main_project.condition.service.Cscore1Service;
import main_project.condition.service.Lscore1Service;
import main_project.trade.service.TradeCodeCatalog;

@Service
@RequiredArgsConstructor
public class InsightProfileService {

    private final JwtUtil jwtUtil;
    private final LoginService loginService;
    private final Cscore1Service cscoreService;
    private final Lscore1Service lscoreService;
    private final TradeCodeCatalog catalog;

    // 로그인 확인
    public MemberDto authenticate(String token) {

        if (token == null || token.isBlank()) {
            throw unauthorized();
        }

        String memberId;

        try {
            memberId = jwtUtil.getMemberIdFromToken(token);
        } catch (RuntimeException exception) {
            throw unauthorized();
        }

        if (memberId == null || memberId.isBlank()) {
            throw unauthorized();
        }

        MemberDto member = loginService.getMyInfo(memberId);

        if (member == null) {
            throw unauthorized();
        }

        memberType(member);

        return member;
    }

    // 회원 유형 확인
    public String memberType(MemberDto member) {

        if (member.getRoleEntity() == null
                || Integer.valueOf(2).equals(
                        member.getRoleEntity().getRoleId()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "수출입기업·물류업체 회원만 이용할 수 있습니다."
            );
        }

        String signupId = member.getSignupEntity() == null
                ? ""
                : member.getSignupEntity().getSignupId();

        if ("201".equals(signupId)) {
            return "SHIPPER";
        }

        if ("301".equals(signupId)) {
            return "LOGISTICS";
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "회원의 업무 유형을 확인할 수 없습니다."
        );
    }

    // 본인의 업무 조건 조회
    @Transactional(readOnly = true)
    public Profile profile(MemberDto member) {

        String type = memberType(member);

        List<Condition> conditions = "SHIPPER".equals(type)
                ? shipperConditions(member.getMemberId())
                : logisticsConditions(member.getMemberId());

        return Profile.builder()
                .memberType(type)
                .conditions(conditions)
                .build();
    }

    private List<Condition> shipperConditions(String memberId) {

        List<Condition> conditions = new ArrayList<>();

        for (var first : cscoreService.cscoreRead(memberId)) {
            Map<String, Object> details = transport(
                    first.getTransportType(),
                    first.getDeparture(),
                    first.getArrival()
            );

            var second = cscoreService.cscore2Read(
                    first.getCscore1Id()
            );

            if (second != null) {
                tons(
                        details,
                        "requestWeight",
                        second.getRequestWeight()
                );

                put(
                        details,
                        "desiredDate",
                        second.getDesiredDate()
                );
            }

            var third = cscoreService.cscore3Read(
                    first.getCscore1Id()
            );

            if (third != null) {
                cargo(
                        details,
                        third.getGeneralContainer(),
                        third.getRefrigerated(),
                        third.getDangerous(),
                        third.getHeavyCargo(),
                        third.getSpecialCargo()
                );
            }

            conditions.add(
                    condition(
                            first.getCscore1Id(),
                            first.getCountryId(),
                            first.getHsCode(),
                            first.getTradeType(),
                            details
                    )
            );
        }

        return conditions;
    }

    private List<Condition> logisticsConditions(String memberId) {

        List<Condition> conditions = new ArrayList<>();

        for (var first : lscoreService.lscoreRead(memberId)) {
            Map<String, Object> details = transport(
                    first.getTransportType(),
                    first.getDeparture(),
                    first.getArrival()
            );

            put(
                    details,
                    "experienceCount",
                    first.getExperienceCount()
            );

            put(details, "regularRoute", first.getRegularRoute());
            put(details, "directRoute", first.getDirectRoute());

            var second = lscoreService.lscore2Read(
                    first.getLscore1Id()
            );

            if (second != null) {
                tons(
                        details,
                        "availableCapacity",
                        second.getAvailableCapacity()
                );

                put(
                        details,
                        "availableDate",
                        second.getAvailableDate()
                );

                put(
                        details,
                        "averageTransitDays",
                        second.getAverageTransitDays()
                );
            }

            var third = lscoreService.lscore3Read(
                    first.getLscore1Id()
            );

            if (third != null) {
                cargo(
                        details,
                        third.getGeneralContainer(),
                        third.getRefrigerated(),
                        third.getDangerous(),
                        third.getHeavyCargo(),
                        third.getSpecialCargo()
                );
            }

            conditions.add(
                    condition(
                            first.getLscore1Id(),
                            first.getCountryId(),
                            first.getHsCode(),
                            first.getTradeType(),
                            details
                    )
            );
        }

        return conditions;
    }

    private Condition condition(
            int id,
            int countryId,
            String rawCode,
            Boolean tradeType,
            Map<String, Object> details
    ) {

        String normalized = catalog.normalize(rawCode);
        String hsCode = normalized == null ? rawCode : normalized;

        // 최종 기준: true = 수출, false = 수입
        String direction = tradeType == null
                ? null
                : (tradeType ? "EXPORT" : "IMPORT");

        Country country = catalog.country(countryId);

        String countryName = country == null
                ? "국가 미확인"
                : country.getName();

        String directionName = direction == null
                ? "방향 미확인"
                : ("EXPORT".equals(direction) ? "수출" : "수입");

        String codeName = hsCode == null || hsCode.isBlank()
                ? "HS 코드 미확인"
                : hsCode;

        return Condition.builder()
                .id(id)
                .countryId(countryId)
                .hsCode(hsCode)
                .direction(direction)
                .label(
                        countryName + " · " + directionName
                                + " · " + codeName
                                + " · 조건 #" + id
                )
                .details(details)
                .build();
    }

    private Map<String, Object> transport(
            Boolean transportType,
            Integer departure,
            Integer arrival
    ) {

        Map<String, Object> details = new LinkedHashMap<>();

        // 최종 기준: true = 해상, false = 항공
        if (transportType != null) {
            details.put(
                    "transport",
                    transportType ? "SEA" : "AIR"
            );
        }

        put(details, "departureId", departure);
        put(details, "arrivalId", arrival);

        return details;
    }

    private void cargo(
            Map<String, Object> details,
            Boolean general,
            Boolean refrigerated,
            Boolean dangerous,
            Boolean heavy,
            Boolean special
    ) {

        put(details, "generalContainer", general);
        put(details, "refrigerated", refrigerated);
        put(details, "dangerous", dangerous);
        put(details, "heavyCargo", heavy);
        put(details, "specialCargo", special);
    }

    private void tons(
            Map<String, Object> details,
            String key,
            Object value
    ) {

        if (value != null) {
            details.put(key, value);
            details.put(key + "Unit", "t");
        }
    }

    private void put(
            Map<String, Object> details,
            String key,
            Object value
    ) {

        if (value == null) {
            return;
        }

        details.put(
                key,
                value instanceof TemporalAccessor
                        ? value.toString()
                        : value
        );
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "로그인이 필요합니다."
        );
    }
}
