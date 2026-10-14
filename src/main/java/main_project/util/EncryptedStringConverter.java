package main_project.util;

import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

// 엔티티 필드에 @Convert(converter = EncryptedStringConverter.class) 를 붙이면
// DB에 저장할 때 자동 암호화, DB에서 읽을 때 자동 복호화된다.
// -> 서비스 코드에서는 평소처럼 getUserEmail() 등으로 평문을 다루면 된다.
// -> Spring Boot가 Hibernate에 스프링 빈을 넘겨주므로 EncryptionUtil을 주입받을 수 있다.
@Component
@Converter
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionUtil encryptionUtil;

    // [1] 엔티티 -> DB : 암호화
    @Override
    public String convertToDatabaseColumn(String attribute) {
        // 엔티티 필드에는 항상 평문만 들어있다고 보고 무조건 암호화한다.
        // ("v1:"로 시작하는 입력을 건너뛰면 사용자가 그런 값을 입력했을 때 평문이 그대로 저장되므로 건너뛰지 않는다)
        return encryptionUtil.encrypt(attribute);
    }

    // [2] DB -> 엔티티 : 복호화
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) { return null; }
        // [임시 허용] "v1:"로 시작하지 않는 값(아직 평문)은 복호화하지 않고 그대로 돌려준다.
        // 이유 : 예전 샘플 SQL은 평문 회원을 넣고 서버 시작 후 암호화했기 때문에, 평문 회원을 읽다가 서버가 죽지 않도록 둔 것.
        //        (지금 샘플 SQL은 암호문을 직접 넣음)
        // 모든 회원이 암호화된 뒤에는 이 분기를 없애고 평문이면 예외를 던지도록 바꾸는 것을 권장.
        if (!encryptionUtil.isEncrypted(dbData)) { return dbData; }
        return encryptionUtil.decrypt(dbData);
    }
}
