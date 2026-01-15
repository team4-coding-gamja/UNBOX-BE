package com.example.unbox_be.domain.user.user.service;

import com.example.unbox_be.domain.user.user.dto.request.AdminUserUpdateRequestDto;
import com.example.unbox_be.domain.user.user.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_be.domain.user.user.dto.response.AdminUserListResponseDto;
import com.example.unbox_be.domain.user.user.dto.response.AdminUserUpdateResponseDto;
import com.example.unbox_be.domain.user.user.mapper.AdminUserMapper;
import com.example.unbox_be.domain.user.user.entity.User;
import com.example.unbox_be.domain.user.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AdminUserServiceImpl implements  AdminUserService {

    private final UserRepository userRepository;
    private final AdminUserMapper AdminUserMapper;


    // ✅ 사용자 목록 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public Page<AdminUserListResponseDto> getAdminUserPage(Pageable pageable) {
        Page<User> users = userRepository.findAllByDeletedAtIsNull(pageable);
        return users.map(AdminUserMapper::toAdminUserListResponseDto);
    }

    // ✅ 사용자 목록 조회(삭제 포함)
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public Page<AdminUserListResponseDto> getAdminUserPageIncludeDeleted(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(AdminUserMapper::toAdminUserListResponseDto);
    }

    // ✅ 사용자 상세 정보 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminUserDetailResponseDto getAdminUserDetail(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return AdminUserMapper.toAdminUserDetailResponseDto(user);
    }

    // ✅ 사용자 상세 정보 수정
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminUserUpdateResponseDto updateAdminUser(Long userId, AdminUserUpdateRequestDto requestDto) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
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
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public void deleteAdminUser(Long userId, String deletedBy) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.softDelete(deletedBy);
    }
}
