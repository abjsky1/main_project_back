package main_project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MypageDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;
import main_project.util.JwtUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class MypageService {

    private final MemberRepository memberRepository;

    private final JwtUtil jwtUtil;


//  내 정보 수정 (이름 · 주소) , 성공하면 true
    public boolean infoUpdate(String memberId, MypageDto mypageDto, String accessToken) {

        // 1. 본인 확인 : 쿠키(AccessToken) 속 회원 번호와 주소의 회원 번호가 같아야 수정 가능
        if (!isLoginMember(accessToken, memberId)) {
            return false;
        }

        // 2. 입력값 확인 : 빈칸 X , DB 칸 길이 이하 (manager_name 50자 , company_address 300자)
        if (mypageDto.getManagerName() == null || mypageDto.getCompanyAddress() == null) {
            return false;
        }

        String managerName = mypageDto.getManagerName().trim();
        String companyAddress = mypageDto.getCompanyAddress().trim();

        if (managerName.isEmpty() || managerName.length() > 50) {
            return false;
        }

        if (companyAddress.isEmpty() || companyAddress.length() > 300) {
            return false;
        }

        // 3. 회원 조회
        MemberEntity memberEntity = memberRepository.findById(memberId).orElse(null);

        if (memberEntity == null) {
            return false;
        }

        // 4. setter 로 바꾸면 트랜잭션이 끝날 때 JPA 가 UPDATE 해줌 (dirty checking — AuthorizationService 와 같은 방식)
        memberEntity.setManagerName(managerName);
        memberEntity.setCompanyAddress(companyAddress);

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
