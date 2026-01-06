package com.example.unbox_be.domain.user.service;

import com.example.unbox_be.domain.user.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserMeResponseDto;
import com.example.unbox_be.domain.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

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
            when(user.getId()).thenReturn(1L);
            when(user.getEmail()).thenReturn("u@u.com");
            when(user.getNickname()).thenReturn("닉");
            when(user.getPhone()).thenReturn("010-1111-2222");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            // when
            UserMeResponseDto result = userService.getUserMe(userId);

            // then
            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("u@u.com");
            assertThat(result.getNickname()).isEqualTo("닉");
            assertThat(result.getPhone()).isEqualTo("010-1111-2222");
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

            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
        }

        @Test
        @DisplayName("검증: 회원이 있으면 user getter들이 호출된다(UserMapper 내부 동작)")
        void 회원정보조회_정상_getter호출검증() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getEmail()).thenReturn("u@u.com");
            when(user.getNickname()).thenReturn("닉");
            when(user.getPhone()).thenReturn("010");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            // when
            userService.getUserMe(userId);

            // then
            verify(user, times(1)).getId();
            verify(user, times(1)).getEmail();
            verify(user, times(1)).getNickname();
            verify(user, times(1)).getPhone();
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
            // mapper가 쓰는 getter 값 세팅
            when(user.getId()).thenReturn(1L);
            when(user.getEmail()).thenReturn("u@u.com");
            when(user.getNickname()).thenReturn("변경닉");
            when(user.getPhone()).thenReturn("010-9999-8888");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            UserMeUpdateRequestDto requestDto = mock(UserMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("변경닉");
            when(requestDto.getPhone()).thenReturn("010-9999-8888");

            // when
            UserMeUpdateResponseDto result = userService.updateUserMe(userId, requestDto);

            // then
            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
            verify(requestDto, times(1)).getNickname();
            verify(requestDto, times(1)).getPhone();
            verify(user, times(1)).updateUser("변경닉", "010-9999-8888");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("u@u.com");
            assertThat(result.getNickname()).isEqualTo("변경닉");
            assertThat(result.getPhone()).isEqualTo("010-9999-8888");
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

            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
            verify(requestDto, never()).getNickname();
            verify(requestDto, never()).getPhone();
        }

        @Test
        @DisplayName("검증: updateUser에 전달되는 값이 requestDto 값과 동일하다(ArgumentCaptor)")
        void 회원정보수정_updateUser_인자검증() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getEmail()).thenReturn("u@u.com");
            when(user.getNickname()).thenReturn("N");
            when(user.getPhone()).thenReturn("P");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            UserMeUpdateRequestDto requestDto = mock(UserMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("N");
            when(requestDto.getPhone()).thenReturn("P");

            ArgumentCaptor<String> nickCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);

            // when
            userService.updateUserMe(userId, requestDto);

            // then
            verify(user).updateUser(nickCaptor.capture(), phoneCaptor.capture());
            assertThat(nickCaptor.getValue()).isEqualTo("N");
            assertThat(phoneCaptor.getValue()).isEqualTo("P");
        }

        @Test
        @DisplayName("엣지: nickname이 null이어도 updateUser(null, phone)로 호출된다")
        void 회원정보수정_닉네임null_updateUser호출() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getEmail()).thenReturn("u@u.com");
            when(user.getNickname()).thenReturn(null);
            when(user.getPhone()).thenReturn("010-1234-5678");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            UserMeUpdateRequestDto requestDto = mock(UserMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn(null);
            when(requestDto.getPhone()).thenReturn("010-1234-5678");

            // when
            userService.updateUserMe(userId, requestDto);

            // then
            verify(user).updateUser(isNull(), eq("010-1234-5678"));
        }

        @Test
        @DisplayName("엣지: phone이 null이어도 updateUser(nickname, null)로 호출된다")
        void 회원정보수정_폰null_updateUser호출() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getEmail()).thenReturn("u@u.com");
            when(user.getNickname()).thenReturn("닉");
            when(user.getPhone()).thenReturn(null);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            UserMeUpdateRequestDto requestDto = mock(UserMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("닉");
            when(requestDto.getPhone()).thenReturn(null);

            // when
            userService.updateUserMe(userId, requestDto);

            // then
            verify(user).updateUser(eq("닉"), isNull());
        }

        @Test
        @DisplayName("엣지: nickname/phone 둘 다 null이어도 updateUser(null, null)로 호출된다")
        void 회원정보수정_둘다null_updateUser호출() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getEmail()).thenReturn("u@u.com");
            when(user.getNickname()).thenReturn(null);
            when(user.getPhone()).thenReturn(null);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            UserMeUpdateRequestDto requestDto = mock(UserMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn(null);
            when(requestDto.getPhone()).thenReturn(null);

            // when
            userService.updateUserMe(userId, requestDto);

            // then
            verify(user).updateUser(isNull(), isNull());
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
            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
            verify(user, times(1)).softDelete("7");
        }

        @Test
        @DisplayName("실패: 회원이 없으면 CustomException 발생, softDelete는 호출되지 않는다")
        void 회원탈퇴_회원없음_CustomException() {
            // given
            Long userId = 999L;
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteUserMe(userId))
                    .isInstanceOf(CustomException.class);

            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
        }

        @Test
        @DisplayName("검증: 탈퇴 시 softDelete에 들어가는 값은 userId의 문자열이다")
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