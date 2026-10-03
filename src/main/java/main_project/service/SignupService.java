package main_project.service;

import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.MemberDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class SignupService {

    private final MemberRepository memberRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    // 회원가입
    public boolean signup(MemberDto memberDto) {

    // memberDto 에 pk 값으로 UUID 를 넣어주기
    String pk = UUID.randomUUID().toString();
    memberDto.setMemberId(pk);

    // 사용자가 입력한 평문 비밀번호를 BCrypt로 해서
    String encodedPassword = passwordEncoder.encode(memberDto.getUserPassword());

    // 암호화된 비밀번호 DTO에 다시 넣는다
    memberDto.setUserPassword(encodedPassword);

    // UUID 가 추가된 memberDto 를 엔티티로 바꿔
    MemberEntity memberEntity = memberDto.toEntity();

    // 엔티티를 리포지토리를 이용해 저장
    MemberEntity savedEntity = memberRepository.save(memberEntity);

    // 저장이 되면 트루 , 아니면 false
    if (savedEntity != null) {
        return true;
    }

    return false;
}




}
