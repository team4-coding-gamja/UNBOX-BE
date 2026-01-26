package com.example.unbox_user.admin.repository;

import com.example.unbox_user.admin.entity.Admin;
import com.example.unbox_user.admin.entity.AdminRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    // ✅ 닉네임 중복 여부 확인
    boolean existsByNickname(String nickname);
    
    // ✅ 이메일 중복 여부 확인 (삭제 포함)
    boolean existsByEmail(String email);

    // ✅ 전화번호 중복 여부 확인 (삭제 포함)
    boolean existsByPhone(String phone);

    // ✅ 특정 역할의 관리자 존재 여부 확인 (예: MASTER 존재 여부)
    boolean existsByAdminRole(AdminRole adminRole);

    // =========================
    // ✅ Soft Delete "제외" 조회 (deleted_at is null)
    // =========================

    // ✅ 이메일로 관리자 조회 (로그인용)
    Optional<Admin> findByEmailAndDeletedAtIsNull(String email);

    // ✅ 관리자 목록 조회(역할 여러 개)
    Page<Admin> findByAdminRoleInAndDeletedAtIsNull(List<AdminRole> roles, Pageable pageable);

    // ✅ 삭제되지 않은 특정 관리자 단건 조회
    Optional<Admin> findByIdAndDeletedAtIsNull(Long id);

    // ✅ 삭제되지 않은 특정 역할 목록 조회(여러 역할)
    Page<Admin> findAllByAdminRoleInAndDeletedAtIsNull(List<AdminRole> roles, Pageable pageable);

    // ✅ 삭제되지 않은 특정 역할 목록 조회(단일 역할)
    Page<Admin> findAllByAdminRoleAndDeletedAtIsNull(AdminRole role, Pageable pageable);

    boolean existsByEmailAndDeletedAtIsNull(String email);
    
    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    boolean existsByPhoneAndDeletedAtIsNull(String phone);
}
