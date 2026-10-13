package main_project.service;

import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.dto.MemberDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;
import main_project.model.entity.RoleEntity;
import main_project.model.entity.SignupEntity;
import main_project.model.repository.RoleRepository;
import main_project.model.repository.SignupRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class SignupService {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final SignupRepository signupRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    // 회원가입
    public boolean signup(MemberDto memberDto) {

     if (memberDto.getSignupEntity() == null) {return false;}

    String signupId = memberDto.getSignupEntity().getSignupId();

    // 공개 회원가입에서는 화주·물류 유형만 허용
    if (!"201".equals(signupId) && !"301".equals(signupId)) {return false;}

    // DB에 등록된 가입 유형과 일반회원 권한 연결
    SignupEntity signup = signupRepository.findById(signupId).orElse(null);

    RoleEntity role = roleRepository.findById(1).orElse(null);

    if (signup == null || role == null) {return false;}

    // 요청에서 받은 권한·상태 대신 서버가 설정
    memberDto.setSignupEntity(signup);
    memberDto.setRoleEntity(role);
    memberDto.setStatus(true);

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
