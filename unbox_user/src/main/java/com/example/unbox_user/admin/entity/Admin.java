package com.example.unbox_user.admin.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "p_admin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Admin extends BaseEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole adminRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminStatus adminStatus;

//    @OneToMany(mappedBy = "admin")
//    private List<Inspection> inspections = new ArrayList<>();

    // 생성자
    private Admin(String email, String encodedPassword, String nickname, String phone, AdminRole adminRole,AdminStatus adminStatus) {
        this.email = email;
        this.password = encodedPassword;
        this.nickname = nickname;
        this.phone = phone;
        this.adminRole = adminRole;
        this.adminStatus = adminStatus;
    }

    // 생성 메서드
    public static Admin createAdmin(String email, String encodedPassword, String nickname, String phone, AdminRole adminRole) {
        validateEmail(email);
        validatePassword(encodedPassword);
        validateNickname(nickname);
        validatePhone(phone);

        return new Admin(email, encodedPassword, nickname, phone, adminRole, AdminStatus.ACTIVE);
    }

    // 수정 메서드
    public void updateAdmin(String nickname, String phone) {
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
