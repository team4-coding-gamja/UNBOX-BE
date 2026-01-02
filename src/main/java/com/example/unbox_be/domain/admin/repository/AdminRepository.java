package com.example.unbox_be.domain.admin.repository;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    //email을 받아 DB 테이블에서 회원을 조회하는 메소드 작성
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByAdminRole(AdminRole adminRole);

    Optional<Admin> findByEmail(String email);
    // ✅ 관리자(매니저, 검수자) 목록 조회
    Page<Admin> findByAdminRoleIn(List<AdminRole> roles, Pageable pageable);
}
