package com.example.unbox_common.security.auth;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Slf4j
public class CustomUserDetails implements UserDetails {

    private final Long userId;   // 일반 유저 PK
    private final Long adminId;  // 관리자 PK

    private final String email;
    private final String password;
    private final String role;

    // ✅ 생성자를 public으로 변경 (외부 서비스에서 직접 주입하기 위해)
    public CustomUserDetails(Long userId, Long adminId, String email, String password, String role) {
        this.userId = userId;
        this.adminId = adminId;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // ✅ JWT 기반(User) 생성: DB 조회 없이 필터에서 사용
    public static CustomUserDetails ofUserIdOnly(Long userId, String email, String role) {
        return new CustomUserDetails(
                userId,
                null,
                email,
                "", // 패스워드 불필요
                role
        );
    }

    // ✅ JWT 기반(Admin) 생성: DB 조회 없이 필터에서 사용
    public static CustomUserDetails ofAdminIdOnly(Long adminId, String email, String role) {
        return new CustomUserDetails(
                null,
                adminId,
                email,
                "",
                role
        );
    }

    public boolean isAdmin() {
        return adminId != null;
    }

    public boolean isUser() {
        return userId != null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}