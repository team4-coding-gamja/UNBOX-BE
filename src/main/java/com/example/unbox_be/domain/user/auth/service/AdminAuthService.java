package com.example.unbox_be.domain.user.auth.service;

import com.example.unbox_be.domain.user.auth.dto.request.AdminSignupRequestDto;
import com.example.unbox_be.domain.user.auth.dto.response.AdminSignupResponseDto;

public interface AdminAuthService{

    // ✅ 회원가입
    AdminSignupResponseDto signup(AdminSignupRequestDto requestDto);
}
