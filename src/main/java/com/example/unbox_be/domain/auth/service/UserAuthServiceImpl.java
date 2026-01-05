package com.example.unbox_be.domain.auth.service;

import com.example.unbox_be.domain.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.auth.dto.response.UserSignupResponseDto;
import com.example.unbox_be.domain.auth.mapper.AuthMapper;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ 회원가입
    @Override
    @Transactional
    public UserSignupResponseDto signup(UserSignupRequestDto requestDto) {
        String email = requestDto.getEmail();
        String password = requestDto.getPassword();

        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.createUser(
                requestDto.getEmail(),
                passwordEncoder.encode(password),
                requestDto.getNickname(),
                requestDto.getPhone()
        );

        try {
        User savedUser = userRepository.save(user);
        return AuthMapper.toUserSignupResponseDto(savedUser);
        } catch (
        DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }
}
