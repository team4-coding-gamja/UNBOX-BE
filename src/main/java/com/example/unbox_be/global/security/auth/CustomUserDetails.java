package com.example.unbox_be.global.security.auth;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Slf4j
public class CustomUserDetails implements UserDetails {  // Spring Security의 UserDetails 인터페이스 구현

    private final String email;
    private final String password;
    private final String role;

    // User용 생성자
    public static CustomUserDetails ofUser(User user) {
        return new CustomUserDetails(
                user.getEmail(),
                user.getPassword(),
                "ROLE_USER"
        );
    }

    // Admin용 생성자 (Admin 엔티티에 맞게 수정)
    public static CustomUserDetails ofAdmin(Admin admin) {
        return new CustomUserDetails(
                admin.getEmail(),
                admin.getPassword(),
                admin.getAdminRole().name()
        );
    }

    // 생성자로 Member 객체를 받아 저장
    public CustomUserDetails(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
        log.info("[CustomUserDetails] CustomUserDetails 생성자 주입: 사용자 ID={}", email);
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        log.info("[CustomUserDetails/getAuthorities] 사용자 권한(ROLE) 반환");
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        // 사용자의 비밀번호 반환
        log.info("[CustomUserDetails/getPassword] 사용자 비밀번호 반환");
        return password;
    }

    @Override
    public String getUsername() {
        // 사용자의 아이디(username) 반환
        log.info("[CustomUserDetails/getUsername] 반환할 사용자 아이디: {}", email);
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정이 만료되지 않았는지 여부 (true: 만료되지 않음)
        log.info("[CustomUserDetails/isAccountNonExpired] 계정 만료 여부 확인 (true로 설정)");
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정이 잠겨있지 않은지 여부 (true: 잠겨있지 않음)
        log.info("[CustomUserDetails/isAccountNonLocked] 계정 잠김 여부 확인 (true로 설정)");
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호가 만료되지 않았는지 여부 (true: 만료되지 않음)
        log.info("[CustomUserDetails/isCredentialsNonExpired] 비밀번호 만료 여부 확인 (true로 설정)");
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 계정이 활성화되어 있는지 여부 (true: 활성화됨)
        log.info("[CustomUserDetails/isEnabled] 계정 활성화 여부 확인 (true로 설정)");
        return true;
    }
}