package com.example.unbox_be.domain.admin.staff.service;

import com.example.unbox_be.domain.admin.dto.response.*;
import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.domain.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.admin.service.AdminStaffServiceImpl;
import com.example.unbox_be.domain.admin.mapper.AdminStaffMapper;
import com.example.unbox_be.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminStaffServiceImplTest {

    @InjectMocks
    private AdminStaffServiceImpl adminStaffService;

    @Mock private AdminRepository adminRepository;
    @Mock private AdminStaffMapper adminStaffMapper;

    // -------------------------------
    // ✅ 관리자(스태프/검수자) 목록 조회
    // -------------------------------
    @Nested
    @DisplayName("getAdminStaffPage() - 관리자(스태프/검수자) 목록 조회")
    class 관리자스태프목록조회 {

        @Test
        @DisplayName("성공: ROLE_MANAGER/ROLE_INSPECTOR만 조회하고 mapper로 변환된 Page를 반환한다")
        void 관리자스태프목록조회_정상_매핑된페이지반환() {
            // given
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

            Admin a1 = mock(Admin.class);
            Admin a2 = mock(Admin.class);
            Page<Admin> repoPage = new PageImpl<>(List.of(a1, a2), pageable, 2);

            when(adminRepository.findAllByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR)),
                    eq(pageable)
            )).thenReturn(repoPage);

            AdminStaffListResponseDto d1 = mock(AdminStaffListResponseDto.class);
            AdminStaffListResponseDto d2 = mock(AdminStaffListResponseDto.class);
            when(adminStaffMapper.toAdminStaffListResponseDto(a1)).thenReturn(d1);
            when(adminStaffMapper.toAdminStaffListResponseDto(a2)).thenReturn(d2);

            // when
            Page<AdminStaffListResponseDto> result = adminStaffService.getAdminStaffPage(pageable);

            // then
            verify(adminRepository, times(1)).findAllByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR)),
                    eq(pageable)
            );
            verify(adminStaffMapper, times(1)).toAdminStaffListResponseDto(a1);
            verify(adminStaffMapper, times(1)).toAdminStaffListResponseDto(a2);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).containsExactly(d1, d2);
        }

        @Test
        @DisplayName("성공: 조회 결과가 비어있으면 빈 Page를 반환하고 mapper는 호출되지 않는다")
        void 관리자스태프목록조회_빈결과_빈페이지반환() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            when(adminRepository.findAllByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR)),
                    eq(pageable)
            )).thenReturn(Page.empty(pageable));

            // when
            Page<AdminStaffListResponseDto> result = adminStaffService.getAdminStaffPage(pageable);

            // then
            verify(adminStaffMapper, never()).toAdminStaffListResponseDto(any());
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // -------------------------------
    // ✅ 관리자(매니저) 목록 조회
    // -------------------------------
    @Nested
    @DisplayName("getAdminManagerPage() - 관리자(매니저) 목록 조회")
    class 관리자매니저목록조회 {

        @Test
        @DisplayName("성공: ROLE_MANAGER만 조회한다")
        void 관리자매니저목록조회_정상_ROLE_MANAGER만조회() {
            // given
            Pageable pageable = PageRequest.of(0, 30);

            Admin a1 = mock(Admin.class);
            Page<Admin> repoPage = new PageImpl<>(List.of(a1), pageable, 1);

            when(adminRepository.findAllByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_MANAGER)),
                    eq(pageable)
            )).thenReturn(repoPage);

            AdminStaffListResponseDto dto = mock(AdminStaffListResponseDto.class);
            when(adminStaffMapper.toAdminStaffListResponseDto(a1)).thenReturn(dto);

            // when
            Page<AdminStaffListResponseDto> result = adminStaffService.getAdminManagerPage(pageable);

            // then
            verify(adminRepository, times(1)).findAllByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_MANAGER)),
                    eq(pageable)
            );
            verify(adminStaffMapper, times(1)).toAdminStaffListResponseDto(a1);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).containsExactly(dto);
        }
    }

    // -------------------------------
    // ✅ 관리자(검수자) 목록 조회
    // -------------------------------
    @Nested
    @DisplayName("getAdminInspectorPage() - 관리자(검수자) 목록 조회")
    class 관리자검수자목록조회 {

        @Test
        @DisplayName("성공: ROLE_INSPECTOR만 조회한다")
        void 관리자검수자목록조회_정상_ROLE_INSPECTOR만조회() {
            // given
            Pageable pageable = PageRequest.of(0, 50);

            Admin a1 = mock(Admin.class);
            Admin a2 = mock(Admin.class);
            Page<Admin> repoPage = new PageImpl<>(List.of(a1, a2), pageable, 2);

            when(adminRepository.findAllByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_INSPECTOR)),
                    eq(pageable)
            )).thenReturn(repoPage);

            when(adminStaffMapper.toAdminStaffListResponseDto(any(Admin.class)))
                    .thenReturn(mock(AdminStaffListResponseDto.class));

            // when
            Page<AdminStaffListResponseDto> result = adminStaffService.getAdminInspectorPage(pageable);

            // then
            verify(adminRepository, times(1)).findAllByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_INSPECTOR)),
                    eq(pageable)
            );
            verify(adminStaffMapper, times(2)).toAdminStaffListResponseDto(any(Admin.class));
            assertThat(result.getContent()).hasSize(2);
        }
    }

    // -------------------------------
    // ✅ 특정 관리자(스태프) 상세 조회
    // -------------------------------
    @Nested
    @DisplayName("getAdminStaffDetail() - 특정 관리자(스태프) 상세 조회")
    class 관리자상세조회 {

        @Test
        @DisplayName("성공: admin을 조회하고 mapper 결과를 반환한다")
        void 관리자상세조회_정상_응답반환() {
            // given
            Long targetAdminId = 10L;

            Admin admin = mock(Admin.class);
            when(adminRepository.findByIdAndDeletedAtIsNull(targetAdminId))
                    .thenReturn(Optional.of(admin));

            AdminStaffDetailResponseDto responseDto = mock(AdminStaffDetailResponseDto.class);
            when(adminStaffMapper.toAdminStaffDetailResponseDto(admin)).thenReturn(responseDto);

            // when
            AdminStaffDetailResponseDto result = adminStaffService.getAdminStaffDetail(targetAdminId);

            // then
            assertThat(result).isSameAs(responseDto);
            verify(adminRepository, times(1)).findByIdAndDeletedAtIsNull(targetAdminId);
            verify(adminStaffMapper, times(1)).toAdminStaffDetailResponseDto(admin);
        }

        @Test
        @DisplayName("실패: admin이 없으면 CustomException(ADMIN_NOT_FOUND)")
        void 관리자상세조회_대상없음_예외발생() {
            // given
            Long targetAdminId = 999L;
            when(adminRepository.findByIdAndDeletedAtIsNull(targetAdminId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminStaffService.getAdminStaffDetail(targetAdminId))
                    .isInstanceOf(CustomException.class);

            verify(adminStaffMapper, never()).toAdminStaffDetailResponseDto(any());
        }
    }

    // -------------------------------
    // ✅ 특정 관리자(스태프) 정보 수정
    // -------------------------------
    @Nested
    @DisplayName("updateAdminStaff() - 특정 관리자(스태프) 정보 수정")
    class 관리자정보수정 {

        @Test
        @DisplayName("성공: admin.updateAdmin 호출 후 mapper 응답 반환")
        void 관리자정보수정_정상_updateAdmin호출_응답반환() {
            // given
            Long targetAdminId = 1L;

            Admin admin = mock(Admin.class);
            when(adminRepository.findByIdAndDeletedAtIsNull(targetAdminId))
                    .thenReturn(Optional.of(admin));

            AdminStaffUpdateRequestDto requestDto = mock(AdminStaffUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("닉네임수정");
            when(requestDto.getPhone()).thenReturn("010-1234-5678");

            AdminStaffUpdateResponseDto responseDto = mock(AdminStaffUpdateResponseDto.class);
            when(adminStaffMapper.toAdminStaffUpdateResponseDto(admin)).thenReturn(responseDto);

            // when
            AdminStaffUpdateResponseDto result = adminStaffService.updateAdminStaff(targetAdminId, requestDto);

            // then
            assertThat(result).isSameAs(responseDto);

            verify(adminRepository, times(1)).findByIdAndDeletedAtIsNull(targetAdminId);
            verify(admin, times(1)).updateAdmin("닉네임수정", "010-1234-5678");
            verify(adminStaffMapper, times(1)).toAdminStaffUpdateResponseDto(admin);
        }

        @Test
        @DisplayName("실패: 대상 admin이 없으면 CustomException(ADMIN_NOT_FOUND), updateAdmin/mapper 호출 없음")
        void 관리자정보수정_대상없음_예외발생_후속호출없음() {
            // given
            Long targetAdminId = 2L;
            when(adminRepository.findByIdAndDeletedAtIsNull(targetAdminId))
                    .thenReturn(Optional.empty());

            AdminStaffUpdateRequestDto requestDto = mock(AdminStaffUpdateRequestDto.class);

            // when & then
            assertThatThrownBy(() -> adminStaffService.updateAdminStaff(targetAdminId, requestDto))
                    .isInstanceOf(CustomException.class);

            verify(adminStaffMapper, never()).toAdminStaffUpdateResponseDto(any());
        }

        @Test
        @DisplayName("검증: requestDto getter가 각 1회씩 호출된다")
        void 관리자정보수정_getter호출횟수검증() {
            // given
            Long targetAdminId = 3L;

            Admin admin = mock(Admin.class);
            when(adminRepository.findByIdAndDeletedAtIsNull(targetAdminId))
                    .thenReturn(Optional.of(admin));

            AdminStaffUpdateRequestDto requestDto = mock(AdminStaffUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("A");
            when(requestDto.getPhone()).thenReturn("B");

            when(adminStaffMapper.toAdminStaffUpdateResponseDto(admin))
                    .thenReturn(mock(AdminStaffUpdateResponseDto.class));

            // when
            adminStaffService.updateAdminStaff(targetAdminId, requestDto);

            // then
            verify(requestDto, times(1)).getNickname();
            verify(requestDto, times(1)).getPhone();
        }
    }

    // -------------------------------
    // ✅ 특정 관리자(스태프) 삭제 (soft delete)
    // -------------------------------
    @Nested
    @DisplayName("deleteAdmin() - 특정 관리자 삭제(소프트삭제)")
    class 관리자삭제 {

        @Test
        @DisplayName("성공: admin.softDelete(deletedBy)가 호출된다")
        void 관리자삭제_정상_softDelete호출() {
            // given
            Long adminId = 10L;
            String deletedBy = "master@unbox.com";

            Admin admin = mock(Admin.class);
            when(adminRepository.findByIdAndDeletedAtIsNull(adminId))
                    .thenReturn(Optional.of(admin));

            // when
            adminStaffService.deleteAdmin(adminId, deletedBy);

            // then
            verify(adminRepository, times(1)).findByIdAndDeletedAtIsNull(adminId);
            verify(admin, times(1)).softDelete(deletedBy);
        }

        @Test
        @DisplayName("실패: 대상 admin이 없으면 CustomException(ADMIN_NOT_FOUND), softDelete 호출 없음")
        void 관리자삭제_대상없음_예외발생() {
            // given
            Long adminId = 999L;
            when(adminRepository.findByIdAndDeletedAtIsNull(adminId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminStaffService.deleteAdmin(adminId, "x"))
                    .isInstanceOf(CustomException.class);

            // then
            // admin이 없으니 softDelete는 당연히 호출될 수 없음
        }
    }

    // -------------------------------
    // ✅ 내 관리자 정보 조회
    // -------------------------------
    @Nested
    @DisplayName("getAdminMe() - 내 관리자 정보 조회")
    class 내정보조회 {

        @Test
        @DisplayName("성공: admin 조회 후 mapper 응답 반환")
        void 내정보조회_정상_응답반환() {
            // given
            Long adminId = 1L;
            Admin admin = mock(Admin.class);
            when(adminRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));

            AdminMeResponseDto responseDto = mock(AdminMeResponseDto.class);
            when(adminStaffMapper.toAdminMeResponseDto(admin)).thenReturn(responseDto);

            // when
            AdminMeResponseDto result = adminStaffService.getAdminMe(adminId);

            // then
            assertThat(result).isSameAs(responseDto);
            verify(adminRepository, times(1)).findByIdAndDeletedAtIsNull(adminId);
            verify(adminStaffMapper, times(1)).toAdminMeResponseDto(admin);
        }

        @Test
        @DisplayName("실패: admin이 없으면 CustomException(ADMIN_NOT_FOUND)")
        void 내정보조회_대상없음_예외발생() {
            // given
            Long adminId = 2L;
            when(adminRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminStaffService.getAdminMe(adminId))
                    .isInstanceOf(CustomException.class);

            verify(adminStaffMapper, never()).toAdminMeResponseDto(any());
        }
    }

    // -------------------------------
    // ✅ 내 관리자 정보 수정
    // -------------------------------
    @Nested
    @DisplayName("updateAdminMe() - 내 관리자 정보 수정")
    class 내정보수정 {

        @Test
        @DisplayName("성공: admin.updateAdmin 호출 후 mapper 응답 반환")
        void 내정보수정_정상_updateAdmin호출_응답반환() {
            // given
            Long adminId = 1L;

            Admin admin = mock(Admin.class);
            when(adminRepository.findByIdAndDeletedAtIsNull(adminId))
                    .thenReturn(Optional.of(admin));

            AdminMeUpdateRequestDto requestDto = mock(AdminMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("내닉변");
            when(requestDto.getPhone()).thenReturn("010-0000-0000");

            AdminMeUpdateResponseDto responseDto = mock(AdminMeUpdateResponseDto.class);
            when(adminStaffMapper.toAdminMeUpdateResponseDto(admin)).thenReturn(responseDto);

            // when
            AdminMeUpdateResponseDto result = adminStaffService.updateAdminMe(adminId, requestDto);

            // then
            assertThat(result).isSameAs(responseDto);
            verify(admin, times(1)).updateAdmin("내닉변", "010-0000-0000");
            verify(adminStaffMapper, times(1)).toAdminMeUpdateResponseDto(admin);
        }

        @Test
        @DisplayName("실패: admin이 없으면 CustomException(ADMIN_NOT_FOUND), updateAdmin/mapper 호출 없음")
        void 내정보수정_대상없음_예외발생() {
            // given
            Long adminId = 999L;
            when(adminRepository.findByIdAndDeletedAtIsNull(adminId))
                    .thenReturn(Optional.empty());

            AdminMeUpdateRequestDto requestDto = mock(AdminMeUpdateRequestDto.class);

            // when & then
            assertThatThrownBy(() -> adminStaffService.updateAdminMe(adminId, requestDto))
                    .isInstanceOf(CustomException.class);

            verify(adminStaffMapper, never()).toAdminMeUpdateResponseDto(any());
        }

        @Test
        @DisplayName("검증: requestDto getter가 각 1회씩 호출된다")
        void 내정보수정_getter호출횟수검증() {
            // given
            Long adminId = 3L;

            Admin admin = mock(Admin.class);
            when(adminRepository.findByIdAndDeletedAtIsNull(adminId))
                    .thenReturn(Optional.of(admin));

            AdminMeUpdateRequestDto requestDto = mock(AdminMeUpdateRequestDto.class);
            when(requestDto.getNickname()).thenReturn("N");
            when(requestDto.getPhone()).thenReturn("P");

            when(adminStaffMapper.toAdminMeUpdateResponseDto(admin))
                    .thenReturn(mock(AdminMeUpdateResponseDto.class));

            // when
            adminStaffService.updateAdminMe(adminId, requestDto);

            // then
            verify(requestDto, times(1)).getNickname();
            verify(requestDto, times(1)).getPhone();
        }
    }

    // -------------------------------
    // ✅ 관리자(스태프/검수자) 목록 조회(삭제 포함) - 현재 구현 그대로 검증
    // -------------------------------
    @Nested
    @DisplayName("getAdminStaffPageIncludeDeleted() - 관리자(스태프/검수자) 목록 조회(삭제 포함)")
    class 관리자스태프목록조회_삭제포함 {

        @Test
        @DisplayName("성공: repository.findByAdminRoleInAndDeletedAtIsNull 호출 후 mapper 변환 Page 반환")
        void 관리자스태프목록조회_삭제포함_정상_매핑된페이지반환() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            Admin a1 = mock(Admin.class);
            Page<Admin> repoPage = new PageImpl<>(List.of(a1), pageable, 1);

            when(adminRepository.findByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR)),
                    eq(pageable)
            )).thenReturn(repoPage);

            AdminStaffListResponseDto dto = mock(AdminStaffListResponseDto.class);
            when(adminStaffMapper.toAdminStaffListResponseDto(a1)).thenReturn(dto);

            // when
            Page<AdminStaffListResponseDto> result = adminStaffService.getAdminStaffPageIncludeDeleted(pageable);

            // then
            verify(adminRepository, times(1)).findByAdminRoleInAndDeletedAtIsNull(
                    eq(List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR)),
                    eq(pageable)
            );
            verify(adminStaffMapper, times(1)).toAdminStaffListResponseDto(a1);
            assertThat(result.getContent()).containsExactly(dto);
        }
    }
}
