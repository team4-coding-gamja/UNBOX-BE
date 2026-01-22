package com.example.unbox_user.auth.service;

import com.example.unbox_user.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_user.auth.dto.response.UserSignupResponseDto;
import com.example.unbox_user.auth.mapper.AuthMapper;
import com.example.unbox_user.user.entity.User;
import com.example.unbox_user.user.repository.UserRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
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

        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
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
