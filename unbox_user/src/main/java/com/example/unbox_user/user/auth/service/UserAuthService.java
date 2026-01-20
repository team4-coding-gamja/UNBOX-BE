package com.example.unbox_user.user.auth.service;

import com.example.unbox_user.user.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_user.user.auth.dto.response.UserSignupResponseDto;

public interface UserAuthService {

    // ✅ 회원가입
    UserSignupResponseDto signup(UserSignupRequestDto requestDto);
}
