package com.example.unbox_user.common.security.token;

import com.example.unbox_common.security.jwt.JwtConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private String key(String email) {
        return JwtConstants.REFRESH_TOKEN_KEY_PREFIX + email;
    }

    public void saveRefreshToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set(
                key(email),
                refreshToken,
                Duration.ofMillis(JwtConstants.REFRESH_TOKEN_EXPIRE_MS)
        );
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(key(email));
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(key(email));
    }

    public boolean exists(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(email)));
    }
}
