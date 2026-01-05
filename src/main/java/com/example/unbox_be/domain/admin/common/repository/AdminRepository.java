package com.example.unbox_be.domain.admin.common.repository;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByAdminRole(AdminRole adminRole);

    Optional<Admin> findByEmail(String email);

    Page<Admin> findByAdminRoleIn(List<AdminRole> roles, Pageable pageable);
}
