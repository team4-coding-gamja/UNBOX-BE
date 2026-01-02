package com.example.unbox_be.domain.auth.service;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.auth.dto.response.AdminSignupResponseDto;
import com.example.unbox_be.domain.auth.dto.request.AdminSignupRequestDto;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.domain.auth.mapper.AuthMapper;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthServiceImpl implements AdminAuthService{

    public AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthServiceImpl(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입 API
    @Transactional
    public AdminSignupResponseDto signup(AdminSignupRequestDto adminSignupRequestDto) {
        String email = adminSignupRequestDto.getEmail();
        String password = adminSignupRequestDto.getPassword();

        // 이메일 중복 확인
        if (adminRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.ADMIN_ALREADY_EXISTS);
        }

        // MASTER는 MASTER를 추가 생성할 수 없음
        if (adminSignupRequestDto.getAdminRole() == AdminRole.ROLE_MASTER) {
            throw new CustomException(ErrorCode.MASTER_CANNOT_CREATE_MASTER);
        }

        // MASTER만 회원가입 가능
        String signupRole = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (!"ROLE_MASTER".equals(signupRole)) {
            throw new CustomException(ErrorCode.ONLY_MASTER_CAN_CREATE_ADMIN);
        }

        // 관리자 객체 생성
        Admin admin = Admin.createAdmin(
                adminSignupRequestDto.getEmail(),
                passwordEncoder.encode(password),
                adminSignupRequestDto.getNickname(),
                adminSignupRequestDto.getPhone(),
                adminSignupRequestDto.getAdminRole()
        );

        // 저장
        Admin savedAdmin = adminRepository.save(admin);

        // Entity -> Dto 변환 후 반환
        return AuthMapper.toAdminSignupResponseDto(savedAdmin);
    }
}
