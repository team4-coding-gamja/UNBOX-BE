package com.example.unbox_be.domain.user.mapper;

import com.example.unbox_be.domain.user.dto.response.UserResponseDto;
import com.example.unbox_be.domain.user.entity.User;

public class UserMapper {

    // Entity -> Dto
    public static UserResponseDto toDto (User user) {
        return UserResponseDto.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }
}
