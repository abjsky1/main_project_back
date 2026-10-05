package main_project.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisTokenService {
    // [1] 레디스 조작 객체 주입
    private final StringRedisTemplate stringRedisTemplate;

    // [2] refresh 토큰 저장 함수
    public void setRefreshToken(String memberId, String token) {
        // key 에는 RT:회원번호 조합한다. // value : refresh 토큰 / 만료기간 : Duration.ofXXX( 수 ) , Duration.ofDays(7) , 7일
        stringRedisTemplate.opsForValue().set("RT:" + memberId, token, Duration.ofDays(7));
    }

    // [3] refresh 토큰 조회 함수
    public String getRefreshToken(String memberId) {
        return stringRedisTemplate.opsForValue().get("RT:" + memberId); // 조회할 key 조합하여 조회
    }

    // [4] refresh 토큰 삭제 함수
    public boolean deleteRefreshToken(String memberId) {
        return stringRedisTemplate.delete("RT:" + memberId); // 삭제할 key 조합하여 조회
    }
}
