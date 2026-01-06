package com.example.unbox_be.domain.auth.service;

import com.example.unbox_be.domain.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserAuthServiceImpl userAuthService;

    @Test
    void 회원가입_요청시_이메일이_이미_존재하면_USER_ALREADY_EXISTS_예외발생() {
        // given
        UserSignupRequestDto dto = UserSignupRequestDto.builder()
                .email("test@example.com")
                .password("pw1234")
                .nickname("nick")
                .phone("010-0000-0000")
                .build();

        when(userRepository.existsByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userAuthService.signup(dto))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode())
                            .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
                });

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void 회원가입_정상요청이면_저장에_성공하고_응답을_반환한다() {
        // given
        UserSignupRequestDto dto = UserSignupRequestDto.builder()
                .email("test@example.com")
                .password("pw1234")
                .nickname("nick")
                .phone("010-0000-0000")
                .build();

        when(userRepository.existsByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("pw1234"))
                .thenReturn("ENC_PW");

        User savedUser = mock(User.class);
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // when
        var result = userAuthService.signup(dto);

        // then
        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("pw1234");
        verify(userRepository).save(any(User.class));
    }
}

