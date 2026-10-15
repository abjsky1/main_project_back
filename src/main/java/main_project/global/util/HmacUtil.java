package main_project.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

// 검색용 HMAC-SHA-256 값 생성 (이메일, 사업자번호에만 사용)
// -> AES-GCM 암호문은 같은 값이라도 매번 달라서 DB에서 "WHERE 이메일 = ?" 검색이 안 된다.
// -> 그래서 같은 입력이면 항상 같은 결과가 나오는 HMAC 값을 따로 저장해 검색·중복확인에 쓴다.
// -> 비밀키가 있어야 계산할 수 있으므로, DB만 유출돼서는 원래 값을 대입해 맞춰보기 어렵다.
// 결과 : 소문자 16진수 64글자
@Component
@RequiredArgsConstructor
public class HmacUtil {

    private static final String ALGORITHM = "HmacSHA256";

    private final Environment environment;
    private SecretKeySpec secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = new SecretKeySpec(
            EncryptionUtil.readKey(environment, "crypto.hmac-key", "MEMBER_HMAC_KEY"), ALGORITHM);
    }

    // 반드시 PiiNormalizer로 정규화한 값을 넣어야 한다.
    public String hash(String normalizedValue) {
        if (normalizedValue == null) { return null; }
        try {
            // Mac 객체는 여러 요청이 동시에 같이 쓰면 안 되므로 호출마다 새로 만든다.
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);
            byte[] result = mac.doFinal(normalizedValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC 검색값 생성에 실패했습니다.", e);
        }
    }
}
