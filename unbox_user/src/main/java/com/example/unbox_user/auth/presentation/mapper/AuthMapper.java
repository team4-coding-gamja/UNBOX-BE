package com.example.unbox_user.auth.presentation.mapper;

import com.example.unbox_user.admin.domain.entity.Admin;
import com.example.unbox_user.auth.presentation.dto.response.AdminSignupResponseDto;
import com.example.unbox_user.auth.presentation.dto.response.UserSignupResponseDto;
import com.example.unbox_user.user.domain.entity.User;

public class AuthMapper {

    public static UserSignupResponseDto toUserSignupResponseDto(User user) {
        return  UserSignupResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }

    public static AdminSignupResponseDto toAdminSignupResponseDto(Admin admin) {
        return  AdminSignupResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .build();
    }
}
