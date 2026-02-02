package com.example.unbox_user.auth.application.service;

import com.example.unbox_user.auth.presentation.dto.request.UserSignupRequestDto;
import com.example.unbox_user.auth.presentation.dto.response.UserSignupResponseDto;

public interface UserAuthService {

    // ✅ 회원가입
    UserSignupResponseDto signup(UserSignupRequestDto requestDto);
}
