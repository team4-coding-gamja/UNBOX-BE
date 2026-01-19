package com.example.unbox_be.domain.user.service;

import com.example.unbox_be.domain.user.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserMeResponseDto;
import com.example.unbox_be.domain.user.dto.response.UserMeUpdateResponseDto;

public interface UserService {

    // ✅ 회원 정보 조회
    UserMeResponseDto getUserMe(Long userId);
    // ✅ 회원 정보 수정
    UserMeUpdateResponseDto updateUserMe(Long userId, UserMeUpdateRequestDto requestDto);
    // ✅ 회원 탈퇴
    void deleteUserMe(Long userId);

    // [Internal System] 엔티티 조회
    com.example.unbox_be.domain.user.entity.User findUser(Long userId);
}
