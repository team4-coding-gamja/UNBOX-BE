package com.example.unbox_user.user.user.service;

import com.example.unbox_user.user.user.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_user.user.user.dto.response.UserMeResponseDto;
import com.example.unbox_user.user.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_user.user.user.entity.User;

public interface UserService {

    // ✅ 회원 정보 조회
    UserMeResponseDto getUserMe(Long userId);
    // ✅ 회원 정보 수정
    UserMeUpdateResponseDto updateUserMe(Long userId, UserMeUpdateRequestDto requestDto);
    // ✅ 회원 탈퇴
    void deleteUserMe(Long userId);

    // [Internal System] 엔티티 조회
    User findUser(Long userId);
}
