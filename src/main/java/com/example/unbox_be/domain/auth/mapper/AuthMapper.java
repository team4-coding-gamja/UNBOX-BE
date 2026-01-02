package com.example.unbox_be.domain.auth.mapper;

import com.example.unbox_be.domain.auth.dto.response.UserSignupResponseDto;
import com.example.unbox_be.domain.user.entity.User;

public class AuthMapper {

    // Entity -> Dto
    public static UserSignupResponseDto toDto(User user) {
        return  UserSignupResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }
}
