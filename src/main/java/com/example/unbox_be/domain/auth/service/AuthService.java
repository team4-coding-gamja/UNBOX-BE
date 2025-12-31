package com.example.unbox_be.domain.auth.service;

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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입 API
    @Transactional
    public UserSignupResponseDto signup(UserSignupRequestDto userSignupRequestDto) {
        String email = userSignupRequestDto.getEmail();
        String password = userSignupRequestDto.getPassword();

        // 이메일 중복 확인
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 회원 객체 생성
        User user = User.createUser(
                userSignupRequestDto.getEmail(),
                passwordEncoder.encode(password),
                userSignupRequestDto.getNickname(),
                userSignupRequestDto.getPhone()
        );

        // 저장
        User savedUser = userRepository.save(user);

        // Entity -> Dto 변환 후 반환
        return AuthMapper.toDto(savedUser);
    }

}
