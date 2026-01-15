package com.example.unbox_be.domain.user.auth.dto.response;

import com.example.unbox_be.domain.user.admin.entity.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSignupResponseDto {

    private Long id;
    private String email;
    private String nickname;
    private String phone;
    private AdminRole adminRole;
}
