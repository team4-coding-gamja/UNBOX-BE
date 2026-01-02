package com.example.unbox_be.domain.auth.service;

import com.example.unbox_be.domain.auth.dto.request.AdminSignupRequestDto;
import com.example.unbox_be.domain.auth.dto.response.AdminSignupResponseDto;

public interface AdminAuthService{
    // 회원가입
    AdminSignupResponseDto signup(AdminSignupRequestDto adminSignupRequestDto);
}
