package main_project.service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.LoginDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class LoginService {

    private final MemberRepository memberRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberEntity login(LoginDto loginDto) {
       
        // 1. 입력한 이메일과 일치하는 회원을 조회
       MemberEntity memberEntity = memberRepository.findByUserEmail(loginDto.getUserEmail()).orElse(null);

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
}

