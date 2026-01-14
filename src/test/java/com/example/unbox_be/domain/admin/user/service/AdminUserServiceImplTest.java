//package com.example.unbox_be.domain.admin.user.service;
//
//import com.example.unbox_be.domain.user.dto.request.AdminUserUpdateRequestDto;
//import com.example.unbox_be.domain.user.dto.response.AdminUserDetailResponseDto;
//import com.example.unbox_be.domain.user.dto.response.AdminUserListResponseDto;
//import com.example.unbox_be.domain.user.dto.response.AdminUserUpdateResponseDto;
//import com.example.unbox_be.domain.user.mapper.AdminUserMapper;
//import com.example.unbox_be.domain.user.entity.User;
//import com.example.unbox_be.domain.user.repository.UserRepository;
//import com.example.unbox_be.domain.user.service.AdminUserServiceImpl;
//import com.example.unbox_be.global.error.exception.CustomException;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AdminUserServiceImplTest {
//
//    @InjectMocks
//    private AdminUserServiceImpl adminUserService;
//
//    @Mock private UserRepository userRepository;
//
//    // ⚠️ 서비스 코드에서 필드명이 "AdminUserMapper" 로 대문자 시작이라 그대로 맞춤
//    @Mock private AdminUserMapper AdminUserMapper;
//
//    // -------------------------------
//    // ✅ 사용자 목록 조회 (삭제 제외)
//    // -------------------------------
//    @Nested
//    @DisplayName("getAdminUserPage() - 사용자 목록 조회(삭제 제외)")
//    class 사용자목록조회_삭제제외 {
//
//        @Test
//        @DisplayName("성공: findAllByDeletedAtIsNull 호출 후 mapper로 Page.map 변환하여 반환한다")
//        void 사용자목록조회_정상_매핑된페이지반환() {
//            // given
//            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
//
//            User u1 = mock(User.class);
//            User u2 = mock(User.class);
//
//            Page<User> repoPage = new PageImpl<>(List.of(u1, u2), pageable, 2);
//
//            when(userRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(repoPage);
//
//            AdminUserListResponseDto d1 = mock(AdminUserListResponseDto.class);
//            AdminUserListResponseDto d2 = mock(AdminUserListResponseDto.class);
//            when(AdminUserMapper.toAdminUserListResponseDto(u1)).thenReturn(d1);
//            when(AdminUserMapper.toAdminUserListResponseDto(u2)).thenReturn(d2);
//
//            // when
//            Page<AdminUserListResponseDto> result = adminUserService.getAdminUserPage(pageable);
//
//            // then
//            verify(userRepository, times(1)).findAllByDeletedAtIsNull(pageable);
//            verify(AdminUserMapper, times(1)).toAdminUserListResponseDto(u1);
//            verify(AdminUserMapper, times(1)).toAdminUserListResponseDto(u2);
//
//            assertThat(result.getTotalElements()).isEqualTo(2);
//            assertThat(result.getContent()).containsExactly(d1, d2);
//            assertThat(result.getNumber()).isEqualTo(0);
//            assertThat(result.getSize()).isEqualTo(10);
//        }
//
//        @Test
//        @DisplayName("성공: 조회 결과가 비어있으면 빈 Page 반환, mapper는 호출되지 않는다")
//        void 사용자목록조회_빈결과_빈페이지반환() {
//            // given
//            Pageable pageable = PageRequest.of(0, 10);
//            when(userRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(Page.empty(pageable));
//
//            // when
//            Page<AdminUserListResponseDto> result = adminUserService.getAdminUserPage(pageable);
//
//            // then
//            verify(userRepository, times(1)).findAllByDeletedAtIsNull(pageable);
//            verify(AdminUserMapper, never()).toAdminUserListResponseDto(any(User.class));
//
//            assertThat(result.getContent()).isEmpty();
//            assertThat(result.getTotalElements()).isZero();
//        }
//
//        @Test
//        @DisplayName("검증: content 개수만큼 mapper가 호출된다")
//        void 사용자목록조회_매퍼호출횟수검증() {
//            // given
//            Pageable pageable = PageRequest.of(0, 30);
//
//            User u1 = mock(User.class);
//            User u2 = mock(User.class);
//            User u3 = mock(User.class);
//
//            when(userRepository.findAllByDeletedAtIsNull(pageable))
//                    .thenReturn(new PageImpl<>(List.of(u1, u2, u3), pageable, 3));
//
//            when(AdminUserMapper.toAdminUserListResponseDto(any(User.class)))
//                    .thenReturn(mock(AdminUserListResponseDto.class));
//
//            // when
//            Page<AdminUserListResponseDto> result = adminUserService.getAdminUserPage(pageable);
//
//            // then
//            verify(AdminUserMapper, times(3)).toAdminUserListResponseDto(any(User.class));
//            assertThat(result.getContent()).hasSize(3);
//        }
//    }
//
//    // -------------------------------
//    // ✅ 사용자 목록 조회 (삭제 포함)
//    // -------------------------------
//    @Nested
//    @DisplayName("getAdminUserPageIncludeDeleted() - 사용자 목록 조회(삭제 포함)")
//    class 사용자목록조회_삭제포함 {
//
//        @Test
//        @DisplayName("성공: findAll 호출 후 mapper로 Page.map 변환하여 반환한다")
//        void 사용자목록조회_삭제포함_정상_매핑된페이지반환() {
//            // given
//            Pageable pageable = PageRequest.of(0, 10);
//
//            User u1 = mock(User.class);
//            User u2 = mock(User.class);
//            Page<User> repoPage = new PageImpl<>(List.of(u1, u2), pageable, 2);
//
//            when(userRepository.findAll(pageable)).thenReturn(repoPage);
//
//            AdminUserListResponseDto d1 = mock(AdminUserListResponseDto.class);
//            AdminUserListResponseDto d2 = mock(AdminUserListResponseDto.class);
//            when(AdminUserMapper.toAdminUserListResponseDto(u1)).thenReturn(d1);
//            when(AdminUserMapper.toAdminUserListResponseDto(u2)).thenReturn(d2);
//
//            // when
//            Page<AdminUserListResponseDto> result = adminUserService.getAdminUserPageIncludeDeleted(pageable);
//
//            // then
//            verify(userRepository, times(1)).findAll(pageable);
//            verify(AdminUserMapper, times(2)).toAdminUserListResponseDto(any(User.class));
//
//            assertThat(result.getTotalElements()).isEqualTo(2);
//            assertThat(result.getContent()).containsExactly(d1, d2);
//        }
//
//        @Test
//        @DisplayName("성공: 조회 결과가 비어있으면 빈 Page 반환, mapper 호출 없음")
//        void 사용자목록조회_삭제포함_빈결과_빈페이지반환() {
//            // given
//            Pageable pageable = PageRequest.of(0, 10);
//            when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));
//
//            // when
//            Page<AdminUserListResponseDto> result = adminUserService.getAdminUserPageIncludeDeleted(pageable);
//
//            // then
//            verify(userRepository, times(1)).findAll(pageable);
//            verify(AdminUserMapper, never()).toAdminUserListResponseDto(any(User.class));
//            assertThat(result.getContent()).isEmpty();
//        }
//    }
//
//    // -------------------------------
//    // ✅ 사용자 상세 조회
//    // -------------------------------
//    @Nested
//    @DisplayName("getAdminUserDetail() - 사용자 상세 조회")
//    class 사용자상세조회 {
//
//        @Test
//        @DisplayName("성공: 유저 조회 후 mapper 응답을 반환한다")
//        void 사용자상세조회_정상_응답반환() {
//            // given
//            Long userId = 1L;
//
//            User user = mock(User.class);
//            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
//
//            AdminUserDetailResponseDto responseDto = mock(AdminUserDetailResponseDto.class);
//            when(AdminUserMapper.toAdminUserDetailResponseDto(user)).thenReturn(responseDto);
//
//            // when
//            AdminUserDetailResponseDto result = adminUserService.getAdminUserDetail(userId);
//
//            // then
//            assertThat(result).isSameAs(responseDto);
//            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
//            verify(AdminUserMapper, times(1)).toAdminUserDetailResponseDto(user);
//        }
//
//        @Test
//        @DisplayName("실패: 유저가 없으면 CustomException 발생, mapper는 호출되지 않는다")
//        void 사용자상세조회_대상없음_예외발생() {
//            // given
//            Long userId = 999L;
//            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() -> adminUserService.getAdminUserDetail(userId))
//                    .isInstanceOf(CustomException.class);
//
//            verify(AdminUserMapper, never()).toAdminUserDetailResponseDto(any(User.class));
//        }
//    }
//
//    // -------------------------------
//    // ✅ 사용자 수정
//    // -------------------------------
//    @Nested
//    @DisplayName("updateAdminUser() - 사용자 수정")
//    class 사용자수정 {
//
//        @Test
//        @DisplayName("성공: 유저 조회 후 updateUser 호출하고 mapper 응답 반환")
//        void 사용자수정_정상_updateUser호출_응답반환() {
//            // given
//            Long userId = 1L;
//
//            User user = mock(User.class);
//            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
//
//            AdminUserUpdateRequestDto requestDto = mock(AdminUserUpdateRequestDto.class);
//            when(requestDto.getNickname()).thenReturn("닉변");
//            when(requestDto.getPhone()).thenReturn("010-1111-2222");
//
//            AdminUserUpdateResponseDto responseDto = mock(AdminUserUpdateResponseDto.class);
//            when(AdminUserMapper.toAdminUserUpdateResponseDto(user)).thenReturn(responseDto);
//
//            // when
//            AdminUserUpdateResponseDto result = adminUserService.updateAdminUser(userId, requestDto);
//
//            // then
//            assertThat(result).isSameAs(responseDto);
//
//            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
//            verify(user, times(1)).updateUser("닉변", "010-1111-2222");
//            verify(AdminUserMapper, times(1)).toAdminUserUpdateResponseDto(user);
//        }
//
//        @Test
//        @DisplayName("실패: 유저가 없으면 CustomException 발생, updateUser/mapper 호출 없음")
//        void 사용자수정_대상없음_예외발생_후속호출없음() {
//            // given
//            Long userId = 2L;
//
//            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
//            AdminUserUpdateRequestDto requestDto = mock(AdminUserUpdateRequestDto.class);
//
//            // when & then
//            assertThatThrownBy(() -> adminUserService.updateAdminUser(userId, requestDto))
//                    .isInstanceOf(CustomException.class);
//
//            verify(AdminUserMapper, never()).toAdminUserUpdateResponseDto(any(User.class));
//        }
//
//        @Test
//        @DisplayName("검증: requestDto getter가 각 1회씩 호출된다")
//        void 사용자수정_getter호출횟수검증() {
//            // given
//            Long userId = 3L;
//
//            User user = mock(User.class);
//            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
//
//            AdminUserUpdateRequestDto requestDto = mock(AdminUserUpdateRequestDto.class);
//            when(requestDto.getNickname()).thenReturn("N");
//            when(requestDto.getPhone()).thenReturn("P");
//
//            when(AdminUserMapper.toAdminUserUpdateResponseDto(user))
//                    .thenReturn(mock(AdminUserUpdateResponseDto.class));
//
//            // when
//            adminUserService.updateAdminUser(userId, requestDto);
//
//            // then
//            verify(requestDto, times(1)).getNickname();
//            verify(requestDto, times(1)).getPhone();
//        }
//
//        @Test
//        @DisplayName("엣지: nickname/phone이 null이어도 현재 로직상 updateUser는 호출된다")
//        void 사용자수정_null값이어도_updateUser호출확인() {
//            // given
//            Long userId = 4L;
//
//            User user = mock(User.class);
//            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
//
//            AdminUserUpdateRequestDto requestDto = mock(AdminUserUpdateRequestDto.class);
//            when(requestDto.getNickname()).thenReturn(null);
//            when(requestDto.getPhone()).thenReturn(null);
//
//            when(AdminUserMapper.toAdminUserUpdateResponseDto(user))
//                    .thenReturn(mock(AdminUserUpdateResponseDto.class));
//
//            // when
//            adminUserService.updateAdminUser(userId, requestDto);
//
//            // then
//            verify(user, times(1)).updateUser(isNull(), isNull());
//        }
//    }
//
//    // -------------------------------
//    // ✅ 사용자 삭제(소프트 삭제)
//    // -------------------------------
//    @Nested
//    @DisplayName("deleteAdminUser() - 사용자 삭제(소프트 삭제)")
//    class 사용자삭제 {
//
//        @Test
//        @DisplayName("성공: 유저 조회 후 softDelete(deletedBy) 호출")
//        void 사용자삭제_정상_softDelete호출() {
//            // given
//            Long userId = 1L;
//            String deletedBy = "admin@unbox.com";
//
//            User user = mock(User.class);
//            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
//
//            // when
//            adminUserService.deleteAdminUser(userId, deletedBy);
//
//            // then
//            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
//            verify(user, times(1)).softDelete(deletedBy);
//        }
//
//        @Test
//        @DisplayName("실패: 유저가 없으면 CustomException 발생, softDelete 호출되지 않는다")
//        void 사용자삭제_대상없음_예외발생() {
//            // given
//            Long userId = 999L;
//            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() -> adminUserService.deleteAdminUser(userId, "x"))
//                    .isInstanceOf(CustomException.class);
//        }
//    }
//}