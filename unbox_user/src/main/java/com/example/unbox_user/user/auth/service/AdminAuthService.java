package com.example.unbox_user.user.auth.service;

import com.example.unbox_user.user.auth.dto.request.AdminSignupRequestDto;
import com.example.unbox_user.user.auth.dto.response.AdminSignupResponseDto;

public interface AdminAuthService{

    // ✅ 회원가입
    AdminSignupResponseDto signup(AdminSignupRequestDto requestDto);
}
