package com.example.unbox_user.common.security.auth;

import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_user.admin.entity.Admin;
import com.example.unbox_user.admin.repository.AdminRepository;
import com.example.unbox_user.user.entity.User;
import com.example.unbox_user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1) 관리자 조회
        Admin admin = adminRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);
        if (admin != null) {
            log.info("[LoadUser] Admin 발견: {}", email);

            // 팩토리 메서드(ofAdmin) 대신 -> 생성자 직접 호출
            return new CustomUserDetails(
                    null,                           // userId
                    admin.getId(),                  // adminId
                    admin.getEmail(),
                    admin.getPassword(),
                    admin.getAdminRole().name()     // role
            );
        }

        // 2) 일반 유저 조회
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("[LoadUser] User 발견: {}", email);

        // 팩토리 메서드(ofUser) 대신 -> 생성자 직접 호출
        return new CustomUserDetails(
                user.getId(),           // userId
                null,                   // adminId
                user.getEmail(),
                user.getPassword(),
                "ROLE_USER"             // role (하드코딩 or Enum 사용)
        );
    }
}