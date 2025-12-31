package com.example.unbox_be.domain.auth.service;

import com.example.unbox_be.domain.auth.dto.request.UserLoginRequestDto;
import com.example.unbox_be.domain.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.auth.dto.response.UserSignupResponseDto;
import com.example.unbox_be.domain.auth.mapper.AuthMapper;
import com.example.unbox_be.domain.user.dto.response.UserResponseDto;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public interface AuthService {

    // 회원가입
    UserSignupResponseDto signup(UserSignupRequestDto userSignupRequestDto);



}
