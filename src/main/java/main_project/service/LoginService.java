package main_project.service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.LoginDto;
import main_project.model.dto.MemberDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;
import main_project.util.HmacUtil;
import main_project.util.PiiNormalizer;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class LoginService {

    private final MemberRepository memberRepository;
    private final HmacUtil hmacUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberEntity login(LoginDto loginDto) {
       
        // 1. 입력한 이메일을 정규화(공백 제거 + 소문자) → HMAC 검색값으로 회원 조회
        //    (이메일 칸은 암호화되어 있어 평문으로는 찾을 수 없음 , "Hong@Mail.com" 으로 입력해도 로그인 가능)
       String userEmail = PiiNormalizer.normalizeEmail(loginDto.getUserEmail());

       // 빈 이메일이면 실패 (null 로 조회하면 "검색값이 비어 있는 회원" 을 찾는 쿼리가 되므로 미리 막음)
       if (userEmail == null || userEmail.isEmpty()) {return null;}

       MemberEntity memberEntity = memberRepository.findByUserEmailHash(hmacUtil.hash(userEmail)).orElse(null);

       // 2. 회원이 존재하지 않으면 로그인 실패
       if(memberEntity == null) {return null;}

       // 3. 사용자가 입력한 비밀번호와 DB의 Bcrypt 비밀번효 비교
       boolean passwordMatch = passwordEncoder.matches(loginDto.getUserPassword(), memberEntity.getUserPassword());

       // 4. 비밀번호 불일치
       if(passwordMatch == false) { return null;}

        // 5. 비활성화 회원이면 로그인 실패
       if (memberEntity.getStatus() == false) { return null;}

       // 6. 로그인 검증 성공
        return memberEntity;

    }

    // 회원 ID로 회원 정보 조회
    public MemberDto getMyInfo(String memberId) {

    // 1. 회원 ID로 조회
    MemberEntity memberEntity = memberRepository.findById(memberId).orElse(null);

    // 2. 해당 회원 ID의 회원이 DB에 없으면 조회 실패
    if (memberEntity == null) {return null;}

    // 3. 비활성 회원이면 재발급 불가
    if (memberEntity.getStatus() == false) {return null;}

    // 4. 회원 정보를 DTO로 변환하여 반환
    return MemberDto.from(memberEntity);
}
}

