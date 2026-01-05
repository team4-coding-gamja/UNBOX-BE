package com.example.unbox_be.domain.admin.common.repository;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    // ✅ 이메일 중복 여부 확인 (회원가입 / 관리자 등록 시)
    boolean existsByEmail(String email);

    // ✅ 닉네임 중복 여부 확인
    boolean existsByNickname(String nickname);

    // ✅ 특정 역할의 관리자 존재 여부 확인
    // (예: MASTER 관리자 존재 여부 체크용)
    boolean existsByAdminRole(AdminRole adminRole);

    // ✅ 이메일로 관리자 조회 (로그인용)
    Optional<Admin> findByEmail(String email);

    // ✅ 관리자 목록 조회 (역할 여러 개 포함, Soft Delete 미적용)
    // ⚠️ 삭제된 관리자도 포함되므로 사용 주의
    Page<Admin> findByAdminRoleIn(List<AdminRole> roles, Pageable pageable);

    // =========================
    // Soft Delete 적용 조회
    // =========================

    // ✅ 삭제되지 않은 관리자 전체 목록 조회
    Page<Admin> findAllByDeletedAtIsNull(Pageable pageable);

    // ✅ 삭제되지 않은 특정 관리자 단건 조회
    Optional<Admin> findByIdAndDeletedAtIsNull(Long id);

    // ✅ 특정 역할 목록 조회 (여러 역할, Soft Delete 적용)
    Page<Admin> findAllByAdminRoleInAndDeletedAtIsNull(List<AdminRole> roles, Pageable pageable);

    // ✅ 특정 역할 목록 조회 (단일 역할, Soft Delete 적용)
    Page<Admin> findAllByAdminRoleAndDeletedAtIsNull(AdminRole role, Pageable pageable);
}
