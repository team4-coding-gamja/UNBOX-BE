package com.example.unbox_be.domain.user.mapper;

import com.example.unbox_be.domain.user.dto.response.UserMeResponseDto;
import com.example.unbox_be.domain.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_be.domain.user.entity.User;

public class UserMapper {

    // Entity -> Dto
    public static UserMeResponseDto toUserMeResponseDto (User user) {
        return UserMeResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }

    // Entity -> Dto
    public static UserMeUpdateResponseDto toUserUpdateResponseDto (User user) {
        return UserMeUpdateResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }
}
