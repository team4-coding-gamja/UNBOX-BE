package com.example.unbox_be.domain.auth.service;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.domain.auth.dto.request.AdminSignupRequestDto;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.example.unbox_be.domain.admin.entity.AdminRole.ROLE_MANAGER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceImplTest {

    @Mock
    AdminRepository adminRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AdminAuthServiceImpl adminAuthService;

    @Test
    @DisplayName("관리자 회원가입 요청이 MASTER면, MASTER_CANNOT_CREATE_MASTER라는 예외 발생")
    void 관리자_회원가입_요청이_MASTER생성이면_MASTER_CANNOT_CREATE_MASTER_예외발생() {
        // given
        AdminSignupRequestDto dto = AdminSignupRequestDto.builder()
                .email("master@example.com")
                .password("pw1234")
                .nickname("nick")
                .phone("010-0000-0000")
                .adminRole(AdminRole.ROLE_MASTER)
                .build();

        // when & then
        CustomException ex = assertThatExceptionOfType(CustomException.class)
                .isThrownBy(() -> adminAuthService.signup(dto)).actual();

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MASTER_CANNOT_CREATE_MASTER);

        // then: 이후 로직은 실행되지 않는다
        verify(adminRepository, never()).existsByEmailAndDeletedAtIsNull(anyString());
        verify(adminRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("관리자 회원가입 요청 시 이메일이 이미 존재하면 ADMIN_ALREADY_EXISTS 예외 발생")
    void 관리자_회원가입_요청시_이메일이_이미_존재하면_ADMIN_ALREADY_EXISTS_예외발생() {
        // given
        AdminSignupRequestDto dto = AdminSignupRequestDto.builder()
                .email("test@example.com")
                .password("pw1234")
                .nickname("nick")
                .phone("010-0000-0000")
                .adminRole(ROLE_MANAGER)
                .build();

        when(adminRepository.existsByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> adminAuthService.signup(dto))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.ADMIN_ALREADY_EXISTS);
                });

        verify(adminRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("관리자 회원가입 정상 요청이면 저장 성공 후, 응답 반환")
    void 관리자_회원가입_정상요청이면_저장성공하고_응답을_반환한다() {
        // given
        AdminSignupRequestDto dto = AdminSignupRequestDto.builder()
                .email("test@example.com")
                .password("pw1234")
                .nickname("nick")
                .phone("010-0000-0000")
                .adminRole(ROLE_MANAGER)
                .build();

        when(adminRepository.existsByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("pw1234"))
                .thenReturn("ENC_PW");

        Admin savedAdmin = mock(Admin.class);
        when(adminRepository.save(any(Admin.class)))
                .thenReturn(savedAdmin);

        // when
        var result = adminAuthService.signup(dto);

        // then
        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("pw1234");
        verify(adminRepository).save(any(Admin.class));
    }

    @Test
    @DisplayName("관리자 회원가입 시점에 동시성 이슈로 중복 발생하면 ADMIN_ALREADY_EXISTS 예외 발생")
    void 관리자_회원가입_동시성_이슈로_인한_중복_발생시_ADMIN_ALREADY_EXISTS_예외발생() {
        // given
        AdminSignupRequestDto dto = AdminSignupRequestDto.builder()
                .email("test@example.com")
                .password("pw1234")
                .nickname("nick")
                .phone("010-0000-0000")
                .adminRole(ROLE_MANAGER)
                .build();

        // 1차 검사 통과
        when(adminRepository.existsByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("pw1234"))
                .thenReturn("ENC_PW");

        // DB save 시점에 예외 발생
        when(adminRepository.save(any(Admin.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate"));

        // when & then
        assertThatThrownBy(() -> adminAuthService.signup(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADMIN_ALREADY_EXISTS);
    }
}
