package main_project.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil { 
        @Value("${jwt.secret}") 
        // application.properties에 있는 jwt.secret 값을 가져온다.
        // -> JWT를 생성할 때 서명하고,
        //    나중에 전달받은 JWT가 우리 서버가 발급한 토큰인지 검증하기 위해 사용한다.
        private String key; 


        private SecretKey secretKey; 
        // JWT 서명에 사용할 비밀키 객체를 담아둘 변수
        // JWT 서명 : 이 토큰이 우리 서버가 발급했고,  중간에 내용이 변경되지 않았다는 것을 검증하기 위한 값
       
    // 비밀 키 객체를 만듦
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    // [1] JWT Access Token 생성
    public String creatToken ( String memberId) {
        String jwt = Jwts.builder()
                    .subject(memberId)
                    .issuedAt(new Date())
                    .expiration(new Date( new Date().getTime()+(60 * 60 * 1000)))
                    .signWith(secretKey)
                    .compact();

                    return jwt;
    }

    // [2]  JWT 토큰 검증 메소드

    public String getMemberIdFromToken( String token ) {

        try{ Claims claims = Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
        String memberId = claims.getSubject();

        return memberId;
        } catch (Exception e) {
            return null;
        }
    }
}
