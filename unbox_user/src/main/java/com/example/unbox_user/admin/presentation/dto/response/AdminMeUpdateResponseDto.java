package com.example.unbox_user.admin.presentation.dto.response;

import com.example.unbox_user.admin.domain.entity.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMeUpdateResponseDto {

    private Long id;
    private String email;
    private String nickname;
    private String phone;
    private AdminRole adminRole;
}
