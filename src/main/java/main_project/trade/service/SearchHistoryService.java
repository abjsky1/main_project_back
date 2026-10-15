package main_project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.SearchDto;
import main_project.model.entity.MemberEntity;
import main_project.model.entity.SearchHistoryEntity;
import main_project.model.repository.MemberRepository;
import main_project.model.repository.SearchHistoryRepository;
import main_project.util.JwtUtil;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional
    public void save(String accessToken, SearchDto searchDto) {

        // 토큰 검증 후 회원 ID 확인하고 토큰이 없거나 만료·검증 실패이면 null
        String memberId = jwtUtil.getMemberIdFromToken(accessToken);

        // 비로그인은 기록하지 않음
        if (memberId == null) {
            return;
        }

        // 실제 회원 조회
        MemberEntity member = memberRepository.findById(memberId).orElse(null);

        // 없는 회원 또는 비활성 회원은 기록하지 않음
        if (member == null || !Boolean.TRUE.equals(member.getStatus())) {
            return;
        }

        // 검색 국가 정리
        String country = searchDto.getCountry();

        if (country == null || country.isBlank()) {
            country = "전체";
        } else {
            country = country.trim();
        }

        // 회원과 검색 조건으로 Entity 생성
        SearchHistoryEntity history = new SearchHistoryEntity(
                member,
                country,
                searchDto.getHscode().trim(),
                searchDto.getStartdate().toString(),
                searchDto.getEnddate().toString()
        );

        // 검색 조건만 DB에 저장
        searchHistoryRepository.save(history);
    }
}
