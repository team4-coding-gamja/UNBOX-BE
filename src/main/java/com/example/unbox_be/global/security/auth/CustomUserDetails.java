package com.example.unbox_be.global.security.auth;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.user.entity.User;
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

    // ✅ 분리
    private final Long userId;   // 일반 유저 PK
    private final Long adminId;  // 관리자 PK

    private final String email;
    private final String password;
    private final String role;

    // ✅ User용 팩토리
    public static CustomUserDetails ofUser(User user) {
        return new CustomUserDetails(
                user.getId(),
                null,
                user.getEmail(),
                user.getPassword(),
                "ROLE_USER"
        );
    }

    // ✅ Admin용 팩토리
    public static CustomUserDetails ofAdmin(Admin admin) {
        return new CustomUserDetails(
                null,
                admin.getId(),
                admin.getEmail(),
                admin.getPassword(),
                admin.getAdminRole().name()
        );
    }

    // ✅ 생성자 private: 외부에서 new 못 하게 막고, 위 팩토리만 사용하게 강제
    private CustomUserDetails(Long userId, Long adminId, String email, String password, String role) {
        this.userId = userId;
        this.adminId = adminId;
        this.email = email;
        this.password = password;
        this.role = role;

        log.info("[CustomUserDetails] created - email={}, role={}, userId={}, adminId={}",
                email, role, userId, adminId);
    }

    // ✅ JWT 기반(User) 생성: DB 조회 없이 principal 만들 때 사용
    public static CustomUserDetails ofUserIdOnly(Long userId, String email, String role) {
        return new CustomUserDetails(
                userId,
                null,
                email,
                "",     // JwtFilter에서는 password 불필요
                role
        );
    }

    // ✅ JWT 기반(Admin) 생성: DB 조회 없이 principal 만들 때 사용
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

    // email을 username처럼 사용
    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
