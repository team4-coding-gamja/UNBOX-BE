package com.example.unbox_be.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserTokenResponseDto {

    private String accessToken;  // 액세스 토큰
    private String refreshToken; // 리프레시 토큰
    private String role;         // 유저 권한 (ROLE_USER, ROLE_MASTER, etc.)
}
