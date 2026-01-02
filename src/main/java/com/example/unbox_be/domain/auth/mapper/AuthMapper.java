package com.example.unbox_be.domain.auth.mapper;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.auth.dto.response.AdminSignupResponseDto;
import com.example.unbox_be.domain.auth.dto.response.UserSignupResponseDto;
import com.example.unbox_be.domain.user.entity.User;

public class AuthMapper {

    // Entity -> Dto
    public static UserSignupResponseDto toUserSignupResponseDto(User user) {
        return  UserSignupResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }

    // Entity -> Dto
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
