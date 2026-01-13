package com.example.unbox_be.domain.user.service;

import com.example.unbox_be.domain.user.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserMeResponseDto;
import com.example.unbox_be.domain.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_be.global.client.user.dto.UserForReviewInfoResponse;

public interface UserService {

    // ✅ 회원 정보 조회
    UserMeResponseDto getUserMe(Long userId);
    // ✅ 회원 정보 수정
    UserMeUpdateResponseDto updateUserMe(Long userId, UserMeUpdateRequestDto requestDto);
    // ✅ 회원 탈퇴
    void deleteUserMe(Long userId);
    // ✅ 회원 조회 (리뷰용)
    UserForReviewInfoResponse getUserForReview (Long id);
    // ✅ 내부 통신용 메서드
    boolean existsUser(Long userId);

    // [Internal System] 엔티티 조회
    com.example.unbox_be.domain.user.entity.User findUser(Long userId);
}
