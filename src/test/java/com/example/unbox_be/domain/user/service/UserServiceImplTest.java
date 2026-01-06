package com.example.unbox_be.domain.user.service;

import com.example.unbox_be.domain.user.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserMeResponseDto;
import com.example.unbox_be.domain.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.mapper.UserMapper;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    // =========================================================
    // ✅ getUserMe(Long userId) - 회원 정보 조회
    // =========================================================
    @Nested
    @DisplayName("getUserMe() - 회원 정보 조회")
    class 회원정보조회 {

        @Test
        @DisplayName("성공: 회원이 존재하면 UserMapper 결과 DTO를 반환한다")
        void 회원정보조회_정상_응답반환() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            UserMeResponseDto responseDto = mock(UserMeResponseDto.class);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(userMapper.toUserMeResponseDto(user)).thenReturn(responseDto);

            // when
            UserMeResponseDto result = userService.getUserMe(userId);

            // then
            assertThat(result).isSameAs(responseDto);

            verify(userRepository).findByIdAndDeletedAtIsNull(userId);
            verify(userMapper).toUserMeResponseDto(user);
            verifyNoMoreInteractions(userRepository, userMapper);
        }

        @Test
        @DisplayName("실패: 회원이 없으면 CustomException 발생")
        void 회원정보조회_회원없음_CustomException() {
            // given
            Long userId = 999L;
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserMe(userId))
                    .isInstanceOf(CustomException.class);

            verify(userRepository).findByIdAndDeletedAtIsNull(userId);
            verifyNoInteractions(userMapper);
        }
    }

    // =========================================================
    // ✅ updateUserMe(Long userId, UserMeUpdateRequestDto requestDto)
    // =========================================================
    @Nested
    @DisplayName("updateUserMe() - 회원 정보 수정")
    class 회원정보수정 {

        @Test
        @DisplayName("성공: 회원이 존재하면 updateUser를 호출하고 수정 응답 DTO를 반환한다")
        void 회원정보수정_정상_updateUser호출_응답반환() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            UserMeUpdateRequestDto requestDto = mock(UserMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("변경닉");
            when(requestDto.getPhone()).thenReturn("010-9999-8888");

            UserMeUpdateResponseDto responseDto = mock(UserMeUpdateResponseDto.class);
            when(userMapper.toUserMeUpdateResponseDto(user)).thenReturn(responseDto);

            // when
            UserMeUpdateResponseDto result = userService.updateUserMe(userId, requestDto);

            // then
            assertThat(result).isSameAs(responseDto);

            verify(userRepository).findByIdAndDeletedAtIsNull(userId);
            verify(requestDto).getNickname();
            verify(requestDto).getPhone();
            verify(user).updateUser("변경닉", "010-9999-8888");
            verify(userMapper).toUserMeUpdateResponseDto(user);
            verifyNoMoreInteractions(userRepository, userMapper, user, requestDto);
        }

        @Test
        @DisplayName("실패: 회원이 없으면 CustomException 발생")
        void 회원정보수정_회원없음_CustomException() {
            // given
            Long userId = 999L;
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            UserMeUpdateRequestDto requestDto = mock(UserMeUpdateRequestDto.class);

            // when & then
            assertThatThrownBy(() -> userService.updateUserMe(userId, requestDto))
                    .isInstanceOf(CustomException.class);

            verify(userRepository).findByIdAndDeletedAtIsNull(userId);
            verifyNoInteractions(userMapper);
            verifyNoInteractions(requestDto);
        }

        @Test
        @DisplayName("검증: updateUser에 전달되는 값이 requestDto 값과 동일하다(ArgumentCaptor)")
        void 회원정보수정_updateUser_인자검증() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            UserMeUpdateRequestDto requestDto = mock(UserMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("N");
            when(requestDto.getPhone()).thenReturn("P");

            when(userMapper.toUserMeUpdateResponseDto(user)).thenReturn(mock(UserMeUpdateResponseDto.class));

            ArgumentCaptor<String> nickCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);

            // when
            userService.updateUserMe(userId, requestDto);

            // then
            verify(user).updateUser(nickCaptor.capture(), phoneCaptor.capture());
            assertThat(nickCaptor.getValue()).isEqualTo("N");
            assertThat(phoneCaptor.getValue()).isEqualTo("P");
        }
    }

    // =========================================================
    // ✅ deleteUserMe(Long userId) - 회원 탈퇴(소프트 삭제)
    // =========================================================
    @Nested
    @DisplayName("deleteUserMe() - 회원 탈퇴")
    class 회원탈퇴 {

        @Test
        @DisplayName("성공: 회원이 존재하면 softDelete(userId.toString())를 호출한다")
        void 회원탈퇴_정상_softDelete호출() {
            // given
            Long userId = 7L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            // when
            userService.deleteUserMe(userId);

            // then
            verify(userRepository).findByIdAndDeletedAtIsNull(userId);
            verify(user).softDelete("7");
            verifyNoMoreInteractions(userRepository, user);
        }

        @Test
        @DisplayName("실패: 회원이 없으면 CustomException 발생")
        void 회원탈퇴_회원없음_CustomException() {
            // given
            Long userId = 999L;
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteUserMe(userId))
                    .isInstanceOf(CustomException.class);

            verify(userRepository).findByIdAndDeletedAtIsNull(userId);
        }

        @Test
        @DisplayName("검증: softDelete에 들어가는 값은 userId의 문자열이다")
        void 회원탈퇴_softDelete_인자검증() {
            // given
            Long userId = 123L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

            // when
            userService.deleteUserMe(userId);

            // then
            verify(user).softDelete(captor.capture());
            assertThat(captor.getValue()).isEqualTo("123");
        }
    }
}