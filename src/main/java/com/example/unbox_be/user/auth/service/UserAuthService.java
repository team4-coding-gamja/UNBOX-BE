package com.example.unbox_be.user.auth.service;

import com.example.unbox_be.user.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_be.user.auth.dto.response.UserSignupResponseDto;

public interface UserAuthService {

    // ✅ 회원가입
    UserSignupResponseDto signup(UserSignupRequestDto requestDto);
}
