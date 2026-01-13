package com.example.unbox_be.domain.user.service;

import com.example.unbox_be.domain.user.dto.response.UserMeResponseDto;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.domain.user.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.mapper.UserMapper;
import com.example.unbox_be.global.client.user.dto.UserForReviewInfoResponse;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // ✅ 회원 정보 조회
    @Transactional(readOnly = true)
    public UserMeResponseDto getUserMe(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserMeResponseDto(user);
    }

    // ✅ 회원 정보 수정
    @Transactional
    public UserMeUpdateResponseDto updateUserMe(Long userId, UserMeUpdateRequestDto requestDto) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateUser(
                requestDto.getNickname(),
                requestDto.getPhone()
        );
        return userMapper.toUserMeUpdateResponseDto(user);
    }

    // ✅ 회원 탈퇴
    // 추후에 JWT 토큰 무효화 처리하기
    @Transactional
    public void deleteUserMe(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.softDelete(userId.toString());
    }

    @Transactional(readOnly = true)
    public User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // ✅ 회원 조회 (리뷰용)
    @Transactional(readOnly = true)
    public UserForReviewInfoResponse getUserForReview(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UserForReviewInfoResponse.from(user);
    }

    // ✅ 내부 통신용 메서드
    public boolean existsUser(Long userId) {
        return userRepository.existsByIdAndDeletedAtIsNull(userId);
    }
}
