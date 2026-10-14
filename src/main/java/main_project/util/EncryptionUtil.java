package main_project.util;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

// 개인정보 AES-256-GCM 암호화 / 복호화
// 저장 모양 : "v1:" + Base64( IV 12바이트 + 암호문 + 인증태그 16바이트 )
// -> "v1:" 은 암호화 방식 버전 표시. 나중에 방식을 바꾸면 "v2:" 로 구분할 수 있다.
// -> GCM 인증태그 : DB 값이 중간에 조작되면 복호화가 실패해서 바로 알 수 있다.
@Component
@RequiredArgsConstructor
public class EncryptionUtil {

    public static final String PREFIX = "v1:";

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;        // GCM 권장 IV 길이 (바이트)
    private static final int TAG_LENGTH_BIT = 128;  // 인증태그 16바이트
    private static final int AES_KEY_LENGTH = 32;   // AES-256 = 32바이트

    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    // 서버 시작 시 키 검사. 문제가 있으면 예외를 던져서 서버 시작을 멈춘다.
    @PostConstruct
    public void init() {
        byte[] aesKey = readKey(environment, "crypto.aes-key", "MEMBER_AES_KEY");
        byte[] hmacKey = readKey(environment, "crypto.hmac-key", "MEMBER_HMAC_KEY");

        // [검사 1] AES 키는 정확히 32바이트
        if (aesKey.length != AES_KEY_LENGTH) {
            throw new IllegalStateException(
                "[암호화 키 오류] MEMBER_AES_KEY는 Base64를 풀었을 때 32바이트여야 합니다. (현재 "
                + aesKey.length + "바이트) .env 파일의 값을 확인하세요.");
        }
        // [검사 2] AES 키와 HMAC 키는 서로 달라야 한다 (하나가 새도 다른 하나는 안전하도록)
        if (MessageDigest.isEqual(aesKey, hmacKey)) {
            throw new IllegalStateException(
                "[암호화 키 오류] MEMBER_AES_KEY와 MEMBER_HMAC_KEY가 같습니다. 서로 다른 값을 사용하세요.");
        }
        this.secretKey = new SecretKeySpec(aesKey, "AES");
    }

    // [1] 암호화 : 평문 -> "v1:..." (같은 평문이라도 매번 IV가 달라서 결과가 다르다)
    public String encrypt(String plainText) {
        if (plainText == null) { return null; }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherTextWithTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] result = ByteBuffer.allocate(iv.length + cipherTextWithTag.length)
                                      .put(iv)
                                      .put(cipherTextWithTag)
                                      .array();
            return PREFIX + Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("개인정보 암호화에 실패했습니다.", e);
        }
    }

    // [2] 복호화 : "v1:..." -> 평문
    public String decrypt(String encrypted) {
        if (encrypted == null) { return null; }
        if (!isEncrypted(encrypted)) {
            throw new IllegalArgumentException("암호화된 값이 아닙니다. (v1: 로 시작하지 않음)");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherTextWithTag = new byte[buffer.remaining()];
            buffer.get(cipherTextWithTag);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(cipherTextWithTag), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException | BufferUnderflowException e) {
            throw new IllegalStateException("개인정보 복호화에 실패했습니다. (키가 다르거나 값이 손상됨)", e);
        }
    }

    // [3] 이미 암호화된 값인지 확인
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    // 환경변수에서 키를 읽어 Base64를 푼다. 기본값은 두지 않는다.
    // (HmacUtil도 같은 방식으로 키를 읽도록 함께 사용)
    static byte[] readKey(Environment environment, String propertyName, String envName) {
        String value;
        try {
            value = environment.getProperty(propertyName);
        } catch (IllegalArgumentException e) {
            // application.properties에는 있지만 ${...} 환경변수가 없는 경우
            throw new IllegalStateException(
                "[암호화 키 오류] 환경변수 " + envName + "가 없습니다. "
                + ".env 파일을 만들고 Git Bash에서 'set -a; source .env; set +a' 후 서버를 실행하세요.");
        }
        if (value == null) {
            throw new IllegalStateException(
                "[암호화 키 오류] application.properties에 " + propertyName + "=${" + envName + "} 줄이 없습니다.");
        }
        if (value.isBlank()) {
            throw new IllegalStateException("[암호화 키 오류] 환경변수 " + envName + "가 비어 있습니다.");
        }
        try {
            return Base64.getDecoder().decode(value.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "[암호화 키 오류] " + envName + "가 올바른 Base64 형식이 아닙니다. .env 파일의 값을 확인하세요.");
        }
    }
}
