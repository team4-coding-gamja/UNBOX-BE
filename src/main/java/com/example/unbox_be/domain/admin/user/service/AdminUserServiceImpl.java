package com.example.unbox_be.domain.admin.user.service;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.admin.user.dto.request.AdminUserUpdateRequestDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserListResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserUpdateResponseDto;
import com.example.unbox_be.domain.admin.user.mapper.AdminUserMapper;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AdminUserServiceImpl implements  AdminUserService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    // ✅ 사용자 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserListResponseDto> getAdminUserPage(String email, int page, int size) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAll(pageable);
        return users.map(AdminUserMapper::toAdminUserListResponseDto);
    }

    // ✅ 사용자 상세 정보 조회
    @Override
    @Transactional
    public AdminUserDetailResponseDto getAdminUserDetail(String email, Long userId) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return AdminUserMapper.toAdminUserDetailResponseDto(user);
    }

    // ✅ 사용자 상세 정보 수정
    @Override
    @Transactional
    public AdminUserUpdateResponseDto updateAdminUser(String email, Long userId, AdminUserUpdateRequestDto requestDto) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateUser(
                requestDto.getNickname(),
                requestDto.getPhone()
        );
        return AdminUserMapper.toAdminUserUpdateResponseDto(user);
    }

    // ✅ 사용자 상세 정보 삭제
    @Override
    @Transactional
    public void deleteAdminUser(String email, Long userId) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(user); // 실무에서는 hard delete보단 soft delete 추천
    }
}
