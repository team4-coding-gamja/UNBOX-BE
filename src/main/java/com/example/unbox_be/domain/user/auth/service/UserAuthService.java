package com.example.unbox_be.domain.user.auth.service;

import com.example.unbox_be.domain.user.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.user.auth.dto.response.UserSignupResponseDto;

public interface UserAuthService {

    // ✅ 회원가입
    UserSignupResponseDto signup(UserSignupRequestDto requestDto);
}
