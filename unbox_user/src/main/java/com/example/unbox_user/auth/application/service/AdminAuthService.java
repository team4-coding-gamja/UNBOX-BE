package com.example.unbox_user.auth.application.service;

import com.example.unbox_user.auth.presentation.dto.request.AdminSignupRequestDto;
import com.example.unbox_user.auth.presentation.dto.response.AdminSignupResponseDto;

public interface AdminAuthService{

    // ✅ 회원가입
    AdminSignupResponseDto signup(AdminSignupRequestDto requestDto);
}
