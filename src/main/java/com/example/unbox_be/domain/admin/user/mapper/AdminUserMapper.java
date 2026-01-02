package com.example.unbox_be.domain.admin.user.mapper;

import com.example.unbox_be.domain.admin.user.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserListResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserUpdateResponseDto;
import com.example.unbox_be.domain.user.entity.User;

public class AdminUserMapper {

    public static AdminUserListResponseDto toAdminUserListResponseDto(User user) {
        return AdminUserListResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }

    public static AdminUserDetailResponseDto toAdminUserDetailResponseDto(User user) {
        return AdminUserDetailResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }

    public static AdminUserUpdateResponseDto toAdminUserUpdateResponseDto(User user) {
        return AdminUserUpdateResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }
}