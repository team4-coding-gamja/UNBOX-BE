package com.example.unbox_be.common.security.jwt;

public final class JwtConstants {

    private JwtConstants() {}

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh";
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

    public static final long ACCESS_TOKEN_EXPIRE_MS = 60L * 60 * 1000;       // 1시간
    public static final long REFRESH_TOKEN_EXPIRE_MS = 60L * 60 * 60 * 1000; // 60시간
    public static final int REFRESH_COOKIE_MAX_AGE_SEC = (int) (60L * 60 * 60); // 60시간(초)

    public static final String DEFAULT_SAMESITE = "Lax"; // same-origin 기준 권장
}
