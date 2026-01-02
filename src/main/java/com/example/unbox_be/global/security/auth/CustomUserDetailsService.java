package com.example.unbox_be.global.security.auth;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    // 생성자를 통해 MemberRepository 의존성 주입
    public CustomUserDetailsService(UserRepository userRepository, AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        log.info("[CustomUserDetailsService] CustomUserDetailsService 생성자 주입");

    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1) 관리자 우선 조회
        Admin admin = adminRepository.findByEmail(email).orElse(null);
        if (admin != null) {
            log.info("[CustomUserDetailsService/loadUserByUsername] Admin 조회 성공, email: {}", email);
            return CustomUserDetails.ofAdmin(admin);
        }

        // 2) 일반 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("[CustomUserDetailsService/loadUserByUsername] CustomUserDetails 객체로 변환하기 전 User 정보 조회, email: {}", email);

        // 조회된 Member 정보를 CustomUserDetails 객체로 변환하여 반환
        return CustomUserDetails.ofUser(user);

    }
}
