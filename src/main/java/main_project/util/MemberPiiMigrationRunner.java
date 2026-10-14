package main_project.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// ==================== [임시] 회원 개인정보 마이그레이션 ====================
// ※ 임시용 클래스 : 평문 회원이 더 이상 들어오지 않게 되면 (샘플 SQL을 암호문으로 바꾸거나 운영 DB 이전이 끝나면) 삭제할 것
//
// 하는 일 : 서버 시작 후 한 번 실행되어, 아직 평문인 회원의
//          이메일 · 담당자 이름 · 사업자번호 · 전화번호를 "v1:..." 암호문으로 바꾸고
//          user_email_hash · business_reg_no_hash 검색값을 채운다.
// 왜 필요 : 샘플 SQL(MainDBSampleData.sql)은 평문 회원을 넣기 때문에 그대로 두면 로그인 · 중복확인이 안 된다.
//
// - 엔티티로 읽으면 EncryptedStringConverter 가 값을 바꿔버려 평문인지 알 수 없으므로 JdbcTemplate 으로 DB 값을 그대로 읽는다.
// - 여러 번 실행해도 안전 : 이미 "v1:" 인 칸은 다시 암호화하지 않는다.
// - 회원가입과 같은 규칙으로 정규화한 뒤 저장한다 (예: 120-81-10001 → 1208110001)
// - 정규화 후 이메일 · 사업자번호가 다른 회원과 겹치면 그 회원은 건너뛰고 경고 로그를 남긴다 (unique 오류로 서버가 멈추지 않게)
// - 전체가 하나의 트랜잭션 : 중간에 오류가 나면 모두 되돌린다
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberPiiMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionUtil encryptionUtil;
    private final HmacUtil hmacUtil;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // 1. 평문이 남아있거나 검색값이 비어있는 회원 조회 (DB 값 그대로)
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT member_id, user_email, manager_name, business_reg_no, user_phone FROM member " +
            "WHERE user_email_hash IS NULL OR business_reg_no_hash IS NULL " +
            "   OR user_email NOT LIKE 'v1:%' OR manager_name NOT LIKE 'v1:%' " +
            "   OR business_reg_no NOT LIKE 'v1:%' OR user_phone NOT LIKE 'v1:%'");

        if (rows.isEmpty()) {
            log.info("[개인정보 마이그레이션] 암호화할 평문 회원이 없습니다.");
            return;
        }

        // 2. 이미 사용 중인 검색값 (겹치는 회원을 미리 걸러내기 위함)
        Set<String> usedEmailHashes = new HashSet<>(jdbcTemplate.queryForList(
            "SELECT user_email_hash FROM member WHERE user_email_hash IS NOT NULL", String.class));
        Set<String> usedBusinessRegNoHashes = new HashSet<>(jdbcTemplate.queryForList(
            "SELECT business_reg_no_hash FROM member WHERE business_reg_no_hash IS NOT NULL", String.class));

        int done = 0;
        for (Map<String, Object> row : rows) {
            String memberId = (String) row.get("member_id");

            // 3. 평문으로 맞춘 뒤 정규화 (이미 암호문인 칸은 복호화해서 평문을 얻음)
            String userEmail = PiiNormalizer.normalizeEmail(toPlain(row.get("user_email")));
            String businessRegNo = keepIfEmpty(PiiNormalizer.normalizeBusinessRegNo(toPlain(row.get("business_reg_no"))), toPlain(row.get("business_reg_no")));
            String userPhone = keepIfEmpty(PiiNormalizer.normalizePhone(toPlain(row.get("user_phone"))), toPlain(row.get("user_phone")));
            String managerName = PiiNormalizer.normalizeName(toPlain(row.get("manager_name")));

            // 4. 검색값 만들기 + 다른 회원과 겹치는지 확인
            String userEmailHash = hmacUtil.hash(userEmail);
            String businessRegNoHash = hmacUtil.hash(businessRegNo);

            if (usedEmailHashes.contains(userEmailHash) || usedBusinessRegNoHashes.contains(businessRegNoHash)) {
                log.warn("[개인정보 마이그레이션] 회원 {} 건너뜀 : 정규화 후 이메일 또는 사업자번호가 다른 회원과 같습니다. 직접 확인이 필요합니다.", memberId);
                continue;
            }
            usedEmailHashes.add(userEmailHash);
            usedBusinessRegNoHashes.add(businessRegNoHash);

            // 5. 암호화해서 저장
            jdbcTemplate.update(
                "UPDATE member SET user_email = ?, manager_name = ?, business_reg_no = ?, user_phone = ?, " +
                "user_email_hash = ?, business_reg_no_hash = ? WHERE member_id = ?",
                encryptionUtil.encrypt(userEmail),
                encryptionUtil.encrypt(managerName),
                encryptionUtil.encrypt(businessRegNo),
                encryptionUtil.encrypt(userPhone),
                userEmailHash,
                businessRegNoHash,
                memberId);
            done++;
        }

        log.info("[개인정보 마이그레이션] 평문 회원 {}명 중 {}명 암호화 완료", rows.size(), done);
    }

    // DB 값이 이미 "v1:" 암호문이면 복호화, 평문이면 그대로
    private String toPlain(Object dbValue) {
        String value = (String) dbValue;
        return encryptionUtil.isEncrypted(value) ? encryptionUtil.decrypt(value) : value;
    }

    // 정규화 결과가 빈 값이면 원래 값을 유지 (예: 공용 계정 GUEST · WITHDRAWN 의 전화번호 "-")
    private String keepIfEmpty(String normalized, String original) {
        return (normalized == null || normalized.isEmpty()) ? original : normalized;
    }
}
