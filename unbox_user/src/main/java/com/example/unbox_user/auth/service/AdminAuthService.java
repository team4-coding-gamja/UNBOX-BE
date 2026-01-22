package com.example.unbox_user.auth.service;

import com.example.unbox_user.auth.dto.request.AdminSignupRequestDto;
import com.example.unbox_user.auth.dto.response.AdminSignupResponseDto;

public interface AdminAuthService{

    // ✅ 회원가입
    AdminSignupResponseDto signup(AdminSignupRequestDto requestDto);
}
