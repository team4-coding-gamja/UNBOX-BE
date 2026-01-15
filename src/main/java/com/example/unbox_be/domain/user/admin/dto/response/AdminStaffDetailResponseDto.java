package com.example.unbox_be.domain.user.admin.dto.response;

import com.example.unbox_be.domain.user.admin.entity.AdminRole;
import com.example.unbox_be.domain.user.admin.entity.AdminStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminStaffDetailResponseDto {

    private Long id;
    private String email;
    private String nickname;
    private String phone;
    private AdminRole adminRole;
    private AdminStatus adminStatus;
}
