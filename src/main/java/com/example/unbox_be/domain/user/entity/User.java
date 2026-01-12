package com.example.unbox_be.domain.user.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nickname", nullable = false, unique = true, length = 10)
    private String nickname;

    @Column(name = "phone", nullable = false)
    private String phone;

    // 생성자
    private User(String email, String encodedPassword, String nickname, String phone) {
        this.email = email;
        this.password = encodedPassword;
        this.nickname = nickname;
        this.phone = phone;
    }

    // 생성 메서드
    public static User createUser(String email, String encodedPassword, String nickname, String phone) {
        validateEmail(email);
        validatePassword(encodedPassword);
        validateNickname(nickname);
        validatePhone(phone);

        return new User(email, encodedPassword, nickname, phone);
    }

    // 수정 메서드
    public void updateUser(String nickname, String phone) {
        validateNickname(nickname);
        validatePhone(phone);

        this.nickname = nickname;
        this.phone = phone;
    }

    // 유효성 검사 메서드
    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
    }

    private static final String NICKNAME_REGEX = "^[a-z0-9]{4,10}$";

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        if (!nickname.matches(NICKNAME_REGEX)) {
            throw new IllegalArgumentException("닉네임은 영문 소문자와 숫자로 이루어진 4~10자여야 합니다.");
        }
    }

    private static void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("전화번호는 필수입니다.");
        }
    }
}
