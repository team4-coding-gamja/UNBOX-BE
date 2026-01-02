package com.example.unbox_be.domain.admin.repository;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    //email을 받아 DB 테이블에서 회원을 조회하는 메소드 작성
    boolean existsByEmail(String email);

    Optional<Admin> findByEmail(String email);

    boolean existsByNickname(String nickname);
    boolean existsByAdminRole(AdminRole adminRole);

}
