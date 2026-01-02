package com.example.unbox_be.domain.user.service;

import com.example.unbox_be.domain.user.dto.request.UserUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserResponseDto;

public interface UserService {

    // 회원 정보 조회
    UserResponseDto getUserByEmail(String email);

    // 회원 정보 수정
    void updateUser(String email, UserUpdateRequestDto userUpdateRequestDto);

    // 회원 탈퇴
    void deleteUser(String email);
}
