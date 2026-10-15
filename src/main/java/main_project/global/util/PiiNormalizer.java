package main_project.global.util;

import java.util.Locale;

// 개인정보(이메일, 전화번호, 사업자번호, 이름) 입력값을 같은 모양으로 맞춰주는 클래스
// -> 같은 사람이 "A@B.com" / "a@b.com " 처럼 다르게 입력해도 같은 값으로 저장·검색되게 한다.
// -> HMAC 검색값은 한 글자만 달라도 완전히 다른 값이 되므로, 해시를 만들기 전에 반드시 정규화해야 한다.
public final class PiiNormalizer {

    private PiiNormalizer() {}

    // [1] 이메일 : 모든 공백 제거 후 소문자
    public static String normalizeEmail(String email) {
        if (email == null) { return null; }
        return email.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    // [2] 전화번호 : 하이픈과 공백 제거
    public static String normalizePhone(String phone) {
        return removeHyphenAndSpace(phone);
    }

    // [3] 사업자번호 : 하이픈과 공백 제거
    public static String normalizeBusinessRegNo(String businessRegNo) {
        return removeHyphenAndSpace(businessRegNo);
    }

    // [4] 이름 : 앞뒤 공백만 제거 (가운데 공백 유지, 대소문자 그대로)
    public static String normalizeName(String name) {
        if (name == null) { return null; }
        return name.strip();
    }

    private static String removeHyphenAndSpace(String value) {
        if (value == null) { return null; }
        return value.replaceAll("[-\\s]", "");
    }
}
