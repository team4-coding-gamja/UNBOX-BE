package com.example.unbox_be.domain.auth.service;

import com.example.unbox_be.domain.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.auth.dto.response.UserSignupResponseDto;

public interface UserAuthService {

    // 회원가입
    UserSignupResponseDto signup(UserSignupRequestDto userSignupRequestDto);
}
