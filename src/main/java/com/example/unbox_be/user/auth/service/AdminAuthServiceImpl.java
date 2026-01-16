package com.example.unbox_be.user.auth.service;

import com.example.unbox_be.user.admin.entity.Admin;
import com.example.unbox_be.user.admin.entity.AdminRole;
import com.example.unbox_be.user.auth.dto.response.AdminSignupResponseDto;
import com.example.unbox_be.user.auth.dto.request.AdminSignupRequestDto;
import com.example.unbox_be.user.admin.repository.AdminRepository;
import com.example.unbox_be.user.auth.mapper.AuthMapper;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService{

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ 회원가입
    @Override
    @Transactional
    @PreAuthorize("hasRole('MASTER')")
    public AdminSignupResponseDto signup(AdminSignupRequestDto requestDto) {
        String email = requestDto.getEmail();
        String password = requestDto.getPassword();

        if (requestDto.getAdminRole() == AdminRole.ROLE_MASTER) {
            throw new CustomException(ErrorCode.MASTER_CANNOT_CREATE_MASTER);
        }
        if (adminRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new CustomException(ErrorCode.ADMIN_ALREADY_EXISTS);
        }

        Admin admin = Admin.createAdmin(
                requestDto.getEmail(),
                passwordEncoder.encode(password),
                requestDto.getNickname(),
                requestDto.getPhone(),
                requestDto.getAdminRole()
        );

        try {
            Admin savedAdmin = adminRepository.save(admin);
            return AuthMapper.toAdminSignupResponseDto(savedAdmin);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ADMIN_ALREADY_EXISTS);
        }
    }
}
