package com.example.unbox_be.global.security.jwt;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtUtil {

    private SecretKey secretKey; // JWT 서명에 사용할 비밀 키
    private final StringRedisTemplate redisTemplate; // Redis 사용

    public JwtUtil(@Value("${spring.jwt.secret}") String secret, StringRedisTemplate redisTemplate) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        this.redisTemplate = redisTemplate;
        log.info("[JWTUtil] JWT secretKey 생성: {}", secretKey);
    }

    // ✅ 토큰에서 userId / adminId 추출 메서드
    public Long getUserId(String token) {
        log.info("[JWTUtil/getUserId] 토큰에서 userId 추출 시도");

        Long userId = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class); // "userId" 키로 저장된 값을 Long으로 꺼냄

        // 🚨 유효성 검사: userId가 없으면 예외 발생 (NPE 방지)
        if (userId == null) {
            log.error("[JWTUtil/getUserId] 토큰에 'userId' Claim이 존재하지 않습니다.");
            // 명확한 예외를 던져서 호출부에서 NPE가 아닌 원인을 알 수 있게 함
            throw new IllegalArgumentException("토큰에 userId 정보가 없습니다.");
        }

        return userId;
    }

    public Long getAdminId(String token) {
        Long adminId = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("adminId", Long.class);

        if (adminId == null) {
            log.error("[JWTUtil/getAdminId] 토큰에 'adminId' Claim이 존재하지 않습니다.");
            throw new IllegalArgumentException("토큰에 adminId 정보가 없습니다.");
        }
        return adminId;
    }

    // 토큰에서 username 추출
    public String getUsername(String token) {
        log.info("[JWTUtil/getUsername] 토큰에서 email 추출, 토큰: {}", token);  // 토큰 정보도 로그에 남김
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }

    // 토큰에서 role 추출
    public String getRole(String token) {
        log.info("[JWTUtil/getRole] 토큰에서 role 추출, 토큰: {}", token);  // 토큰 정보도 로그에 남김
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }


    // 토큰에서 provider 추출
    public String getProvider(String token) {
        log.info("[JWTUtil/getProvider] 토큰에서 provider 추출, 토큰: {}", token);
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("provider", String.class);
    }

    // 토큰에서 providerId 추출
    public String getProviderId(String token) {
        log.info("[JWTUtil/getProvider] 토큰에서 provider 추출, 토큰: {}", token);
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("providerId", String.class);
    }

    // 블랙리스트 확인 - 로그아웃 된거 확인
    public boolean isBlacklisted(String token) {
        String key = "blacklist:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    // 토큰 블랙리스트 추가 (로그아웃 시 사용)
    public void addToBlacklist(String token, long expirationMillis) {
        String key = "blacklist:" + token;
        redisTemplate.opsForValue().set(key, "true", expirationMillis, TimeUnit.MILLISECONDS);
        log.info("[JWTUtil] 블랙리스트(로그아웃)에 토큰 추가: {}, 만료 시간: {} ms", token, expirationMillis);
    }

    // 토큰이 만료되었는지 확인
    public Boolean isExpired(String token) {
        log.info("[JWTUtil/isExpired] 토큰 만료 여부 확인, 토큰: {}", token);  // 토큰 정보도 로그에 남김
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date());
    }

    // 사용자 - 새로운 JWT 생성 (email, role, 만료 시간 지정)
    public String createAccessToken(Long userId, String email, String role, Long expiredMs) {
        log.info("[JWTUtil/createJwt] 새로운 JWT 생성, username: {}, role: {}, 만료 시간(ms): {}", email, role, expiredMs);
        return Jwts.builder()
                .claim("userId", userId)
                .claim("email", email) // email 클레임 추가
                .claim("role", role) // role 클레임 추가
                .issuedAt(new Date(System.currentTimeMillis())) // 발급 시간 설정
                .expiration(new Date(System.currentTimeMillis() + expiredMs)) // 만료 시간 설정
                .signWith(secretKey) // 서명 추가
                .compact(); // 토큰 생성 및 반환
    }

    public String createRefreshToken(Long userId, String email, String role, Long expiredMs) {
        log.info("[JWTUtil/createRefreshToken] 새로운 리프레시 토큰 생성, email: {}, 만료 시간(ms): {}", email, expiredMs);
        return Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs)) // 리프레시 토큰 만료 시간
                .signWith(secretKey)
                .compact();
    }

    // 관리자 - 새로운 JWT 생성 (email, role, 만료 시간 지정)
    public String createAdminAccessToken(Long adminId, String email, String role, Long expiredMs) {
        return Jwts.builder()
                .claim("adminId", adminId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

    public String createAdminRefreshToken(Long adminId, String email, String role, Long expiredMs) {
        return Jwts.builder()
                .claim("adminId", adminId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

}
