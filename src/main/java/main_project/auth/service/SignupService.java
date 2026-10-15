package main_project.auth.service;

import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.member.dto.MemberDto;
import main_project.member.entity.MemberEntity;
import main_project.member.repository.MemberRepository;
import main_project.member.entity.RoleEntity;
import main_project.member.entity.SignupEntity;
import main_project.member.repository.RoleRepository;
import main_project.member.repository.SignupRepository;
import main_project.global.util.HmacUtil;
import main_project.global.util.PiiNormalizer;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class SignupService {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final SignupRepository signupRepository;
    private final HmacUtil hmacUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    // 회원가입
    public boolean signup(MemberDto memberDto) {

     if (memberDto.getSignupEntity() == null) {return false;}

    String signupId = memberDto.getSignupEntity().getSignupId();

    // 공개 회원가입에서는 화주·물류 유형만 허용
    if (!"201".equals(signupId) && !"301".equals(signupId)) {return false;}

    // DB에 등록된 가입 유형과 일반회원 권한 연결
    SignupEntity signup = signupRepository.findById(signupId).orElse(null);

    RoleEntity role = roleRepository.findById(RoleEntity.USER_ROLE_ID).orElse(null);   // 일반 회원 권한 (role 1)

    if (signup == null || role == null) {return false;}

    // 개인정보 정규화 (같은 사람이 다르게 입력해도 같은 값으로 저장 · 중복확인 되도록)
    //  - 이메일 : 공백 제거 + 소문자 , 사업자번호 · 전화번호 : 하이픈 · 공백 제거 , 이름 : 앞뒤 공백만 제거
    String userEmail = PiiNormalizer.normalizeEmail(memberDto.getUserEmail());
    String businessRegNo = PiiNormalizer.normalizeBusinessRegNo(memberDto.getBusinessRegNo());
    String userPhone = PiiNormalizer.normalizePhone(memberDto.getUserPhone());
    String managerName = PiiNormalizer.normalizeName(memberDto.getManagerName());

    // 빈 값 · 길이 확인 (암호화 전 평문 기준 , 예전 DB 칸 길이와 같은 제한)
    //  - 해시 칸이 null 이면 중복확인이 안 되므로 이메일 · 사업자번호는 반드시 있어야 함
    if (isBlankOrTooLong(userEmail, 100) || isBlankOrTooLong(businessRegNo, 12)
            || isBlankOrTooLong(userPhone, 20) || isBlankOrTooLong(managerName, 50)
            || memberDto.getUserPassword() == null || memberDto.getUserPassword().isBlank()) {
        return false;
    }

    // HMAC 검색값으로 중복 확인 (암호문은 매번 달라서 이메일 칸으로는 비교 불가)
    String userEmailHash = hmacUtil.hash(userEmail);
    String businessRegNoHash = hmacUtil.hash(businessRegNo);

    if (memberRepository.existsByUserEmailHash(userEmailHash)) {return false;}           // 이미 가입된 이메일
    if (memberRepository.existsByBusinessRegNoHash(businessRegNoHash)) {return false;}   // 이미 가입된 사업자번호

    // 정규화한 값으로 다시 넣기 (저장할 때 EncryptedStringConverter 가 자동 암호화)
    memberDto.setUserEmail(userEmail);
    memberDto.setBusinessRegNo(businessRegNo);
    memberDto.setUserPhone(userPhone);
    memberDto.setManagerName(managerName);

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

    // 검색값은 DTO 에 칸이 없으니 엔티티에 직접 넣기
    memberEntity.setUserEmailHash(userEmailHash);
    memberEntity.setBusinessRegNoHash(businessRegNoHash);

    // 엔티티를 리포지토리를 이용해 저장
    // (동시에 같은 이메일로 가입하면 위 중복확인을 둘 다 통과할 수 있지만 , DB unique 가 한 건만 저장되게 막는다)
    MemberEntity savedEntity = memberRepository.save(memberEntity);

    // 저장이 되면 트루 , 아니면 false
    if (savedEntity != null) {
        return true;
    }
    return false;
}

    // null · 빈 값이거나 최대 길이를 넘으면 true
    private boolean isBlankOrTooLong(String value, int maxLength) {
        return value == null || value.isEmpty() || value.length() > maxLength;
    }


}
