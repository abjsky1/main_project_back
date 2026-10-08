package main_project.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.InterestDto;
import main_project.model.entity.InterestEntity;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.InterestRepository;
import main_project.model.repository.MemberRepository;
import main_project.util.JwtUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class InterestService {

//  회원 1명당 관심 국가 최대 개수
    private static final int MAX_INTEREST = 3;

    private final InterestRepository interestRepository;

    private final MemberRepository memberRepository;

    private final JwtUtil jwtUtil;

//  국가 번호 → 국가 이름 (서버가 켜질 때 country.csv 를 한 번 읽어서 보관)
//  예) 1233 → "미국"
    private final Map<Integer, String> countryNames = new HashMap<>();


//  [0] 서버 시작 시 country.csv 읽기 (SearchService 가 hscode.csv 를 읽는 방식과 같음)
    @PostConstruct
    public void initialize() {

        ClassPathResource file = new ClassPathResource("static/country/country.csv");

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // 첫 번째 행은 country_id, country_code, currency_id, country_name 제목이므로 제외
            reader.readNext();

            String[] row;

            // 한 행씩 읽고, 더 읽을 행이 없으면 반복 종료
            while ((row = reader.readNext()) != null) {

                // row[0]: 국가 번호 , row[3]: 국가 이름 — 열이 모자라거나 번호가 비어 있으면 건너뛰기
                if (row.length < 4 || row[0].isBlank()) {
                    continue;
                }

                countryNames.put(Integer.parseInt(row[0].trim()), row[3].trim());
            }

        } catch (IOException | CsvValidationException e) {
            throw new IllegalStateException("국가 CSV를 읽을 수 없습니다.", e);
        }
    }


//  [1] 관심 국가 목록 조회 (등록한 순서대로)
    @Transactional(readOnly = true)
    public List<InterestDto> interestRead(String memberId) {

        // 1. 회원의 관심 국가 조회 (interest_id 오름차순 = 먼저 등록한 것부터)
        List<InterestEntity> interestEntities = interestRepository.findByMemberEntityMemberIdOrderByInterestIdAsc(memberId);

        // 2. 엔티티 → DTO (국가 번호를 국가 이름으로 바꿔서 같이 담기)
        List<InterestDto> result = new ArrayList<>();

        for (InterestEntity interestEntity : interestEntities) {

            String countryName = countryNames.get(interestEntity.getCountryId());   // CSV 에 없는 번호면 null

            result.add(InterestDto.from(interestEntity, countryName));
        }

        return result;
    }


//  [2] 관심 국가 추가 (밀어내기)
//  - 3개가 차 있으면 가장 먼저 등록한 국가를 지우고 새 국가를 추가
//  - 본인(로그인한 회원)만 추가 가능 , 성공하면 true
    public boolean interestWrite(InterestDto interestDto, String accessToken) {

        // 1. 본인 확인 : 쿠키(AccessToken) 속 회원 번호와 요청한 회원 번호가 같아야 함
        if (!isLoginMember(accessToken, interestDto.getMemberId())) {
            return false;
        }

        // 2. country.csv 에 있는 국가 번호인지 확인
        if (interestDto.getCountryId() == null || !countryNames.containsKey(interestDto.getCountryId())) {
            return false;
        }

        // 3. 실제로 있는 회원인지 확인
        MemberEntity memberEntity = memberRepository.findById(interestDto.getMemberId()).orElse(null);

        if (memberEntity == null) {
            return false;
        }

        // 4. 지금 등록된 관심 국가 (먼저 등록한 것부터)
        List<InterestEntity> interestEntities = interestRepository.findByMemberEntityMemberIdOrderByInterestIdAsc(interestDto.getMemberId());

        // 이미 등록한 국가면 추가하지 않음
        for (InterestEntity interestEntity : interestEntities) {

            if (interestEntity.getCountryId().equals(interestDto.getCountryId())) {
                return false;
            }
        }

        // 5. 밀어내기 : 이미 3개가 차 있으면 가장 먼저 등록한 국가(목록의 첫 번째)를 삭제
        //    (PK 가 자동 증가라서 "삭제 후 새로 저장"을 하면 등록 순서가 항상 PK 순서와 같게 유지됨)
        if (interestEntities.size() >= MAX_INTEREST) {
            interestRepository.delete(interestEntities.get(0));
        }

        // 6. 새 관심 국가 저장
        InterestEntity savedEntity = interestRepository.save(interestDto.toEntity(memberEntity));

        // 저장 후 PK 가 생성되면 정상 저장
        if (savedEntity.getInterestId() != null) {
            return true;
        }

        return false;
    }


//  [3] 관심 국가 삭제 (본인 것만 삭제 가능 , 성공하면 true)
    public boolean interestDelete(Integer interestId, String accessToken) {

        // 1. 삭제할 관심 국가 조회
        InterestEntity interestEntity = interestRepository.findById(interestId).orElse(null);

        if (interestEntity == null) {
            return false;
        }

        // 2. 본인 확인 : 쿠키 속 회원과 관심 국가의 주인이 같아야 함
        if (!isLoginMember(accessToken, interestEntity.getMemberEntity().getMemberId())) {
            return false;
        }

        // 3. 삭제
        interestRepository.delete(interestEntity);

        return true;
    }


//  쿠키(AccessToken) 속 회원 번호 == 요청한 회원 번호 인지 확인
//  쿠키가 없거나 , 만료됐거나 , 가짜 토큰이면 false
//  (나중에 AOP 에서 토큰 검증을 한 번에 하게 되면 이 메소드는 그쪽으로 옮길 예정)
    private boolean isLoginMember(String accessToken, String memberId) {

        if (accessToken == null || memberId == null) {
            return false;
        }

        String tokenMemberId = jwtUtil.getMemberIdFromToken(accessToken);

        return memberId.equals(tokenMemberId);
    }

}
