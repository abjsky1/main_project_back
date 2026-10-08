package main_project.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.MypageDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.AuditRepository;
import main_project.model.repository.MemberRepository;
import main_project.util.JwtUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class MypageService {

//  role 테이블 번호 (MainDBSampleData.sql : 2 = ROLE_ADMIN)
    private static final int ROLE_ADMIN_ID = 2;

    private final MemberRepository memberRepository;

    private final AuditRepository auditRepository;

    private final JwtUtil jwtUtil;

//  비밀번호 비교용 BCrypt 객체 (LoginService 와 같은 방식)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


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


//  회원 탈퇴 , 성공하면 true
//  - 회원은 DB 에서 삭제 → 매칭 조건 · 매칭 결과 · 관심 국가는 cascade 로 같이 삭제 (MemberEntity 아래쪽 참고)
//  - 감사 로그만 "탈퇴 회원" 공통 계정으로 옮겨서 남김
//  - 쿠키 · 레디스 토큰 삭제는 컨트롤러가 함 (이 메소드가 성공해서 DB 삭제가 끝난 뒤에)
    public boolean withdraw(String accessToken, String userPassword) {

        // 1. 쿠키가 없으면 비로그인 → 실패
        if (accessToken == null) { return false; }

        // 2. 쿠키 속 토큰에서 회원 번호 꺼내기 (만료 · 가짜 토큰이면 null)
        //    ※ 탈퇴할 회원은 주소나 body 가 아니라 쿠키로만 정함 → 다른 사람의 계정은 탈퇴시킬 수 없음
        String memberId = jwtUtil.getMemberIdFromToken(accessToken);
        if (memberId == null) { return false; }

        // 3. 회원 조회 (없는 회원이면 실패)
        MemberEntity memberEntity = memberRepository.findById(memberId).orElse(null);
        if (memberEntity == null) { return false; }

        // 4. 관리자는 탈퇴 불가 (관리자 계정이 사라지면 시스템 관리를 할 사람이 없어짐)
        if (memberEntity.getRoleEntity().getRoleId() == ROLE_ADMIN_ID) { return false; }

        // 5. 비밀번호 확인 (LoginService 의 로그인 검증과 같은 방법)
        //    passwordEncoder.matches( 입력한 비밀번호(평문) , DB 에 저장된 비밀번호(암호문) )
        //    → BCrypt 는 암호문을 다시 평문으로 되돌릴 수 없어서 , 입력값을 같은 방식으로 암호화해 비교함
        if (userPassword == null || userPassword.isEmpty()) { return false; }

        boolean passwordMatch = passwordEncoder.matches(userPassword, memberEntity.getUserPassword());
        if (passwordMatch == false) { return false; }

        // 6. 감사 로그를 "탈퇴 회원" 공통 계정으로 옮기기 (회원을 지워도 로그는 남기기 위해)
        //    audit.member_id 는 FK 라서 , 옮기지 않고 회원을 지우면 DB 가 삭제를 거부함
        //    공통 계정이 DB 에 없으면 로그를 옮길 곳이 없어서 중단
        if (!memberRepository.existsById(AuditService.WITHDRAWN_MEMBER_ID)) { return false; }

        //    로그가 몇 건이든 UPDATE 쿼리 1번으로 옮김 (AuditRepository.moveLogs — 네이티브 쿼리)
        //    UPDATE audit SET member_id = 'WITHDRAWN' WHERE member_id = '탈퇴하는 회원 번호'
        //    ※ 로그를 List 로 가져와서 for 문으로 setter 를 부르면 , 로그 수만큼 UPDATE 가 실행되어 오래 쓴 회원일수록 느려짐
        auditRepository.moveLogs(memberId, AuditService.WITHDRAWN_MEMBER_ID);

        // 7. 회원 삭제
        //    MemberEntity 의 cascade = CascadeType.REMOVE 덕분에 아래 자식 데이터도 같이 삭제됨
        //      회원 ─┬─ 화주 매칭 조건(cscore1) ─┬─ 2번 · 3번 조건 (cscore2 , cscore3)
        //            │                           └─ 매칭 결과 (matching)
        //            ├─ 물류 매칭 조건(lscore1) ─┬─ 2번 · 3번 조건 (lscore2 , lscore3)
        //            │                           └─ 매칭 결과 (matching)
        //            └─ 관심 국가 (interest)
        //    (JPA 가 자식 → 부모 순서로 DELETE 해 줌 . 6번의 로그 UPDATE 는 위에서 이미 실행이 끝난 상태)
        memberRepository.delete(memberEntity);

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
