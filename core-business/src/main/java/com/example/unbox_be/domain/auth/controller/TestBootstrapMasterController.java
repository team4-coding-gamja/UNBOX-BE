package com.example.unbox_be.domain.auth.controller;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.auth.controller.api.TestBootstrapMasterControllerApi;
import com.example.unbox_be.domain.auth.dto.response.AdminSignupResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/bootstrap") // ⚠️ 테스트용, 운영 시 삭제
public class TestBootstrapMasterController implements TestBootstrapMasterControllerApi {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ✅ 테스트용: 마스터 계정 1회 생성 API
     * - 이미 ROLE_MASTER 존재 시 생성 불가
     * - role/status는 서버에서 고정
     */
    @PostMapping("/master")
    @Transactional
    public ResponseEntity<?> createMaster(
            @Valid @RequestBody CreateMasterRequest req
    ) {

        // 1️⃣ 이미 마스터 존재하면 차단
        if (adminRepository.existsByAdminRole(AdminRole.ROLE_MASTER)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("이미 ROLE_MASTER 계정이 존재합니다.");
        }

        // 2️⃣ 중복 체크
        if (adminRepository.existsByEmailAndDeletedAtIsNull(req.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("이미 사용 중인 이메일입니다.");
        }

        if (adminRepository.existsByNickname(req.getNickname())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("이미 사용 중인 닉네임입니다.");
        }

        // 3️⃣ 마스터 계정 생성 (role/status 서버 고정)
        Admin admin = Admin.createAdmin(
                req.getEmail(),
                passwordEncoder.encode(req.getPassword()),
                req.getNickname(),
                req.getPhone(),
                AdminRole.ROLE_MASTER
        );

        Admin saved = adminRepository.save(admin);

        // 4️⃣ 응답 DTO
        AdminSignupResponseDto response = AdminSignupResponseDto.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .nickname(saved.getNickname())
                .phone(saved.getPhone())
                .adminRole(saved.getAdminRole())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // -----------------------------
    // Inner DTO (테스트용)
    // -----------------------------

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateMasterRequest {

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Pattern(
                regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]{8,15}$",
                message = "비밀번호는 8~15자 영문/숫자/특수문자 조합"
        )
        private String password;

        @NotBlank
        private String nickname;

        @NotBlank
        private String phone;
    }
}
