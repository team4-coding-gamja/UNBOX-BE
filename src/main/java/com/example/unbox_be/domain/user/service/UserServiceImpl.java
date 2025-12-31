package com.example.unbox_be.domain.user.service;

import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.domain.user.dto.request.UserUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserResponseDto;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.mapper.UserMapper;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 회원 정보 조회 API
    @Transactional
    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserMapper.toDto(user);
    }

    // 회원 정보 수정 API
    @Transactional
    public void updateUser(String email, UserUpdateRequestDto userUpdateRequestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateUser(
                userUpdateRequestDto.getNickname(),
                userUpdateRequestDto.getPhone()
        );
    }
    
    // 회원 탈퇴 API
    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
}
