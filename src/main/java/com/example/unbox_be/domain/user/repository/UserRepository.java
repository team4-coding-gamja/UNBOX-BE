package com.example.unbox_be.domain.user.repository;

import com.example.unbox_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ 이메일 중복 체크 - 삭제 포함(주의)
    boolean existsByEmail(String email);

    // ✅ 이메일 조회 - 삭제 포함(주의)
    Optional<User> findByEmail(String email);

    // ✅ 유저 전체 페이징 조회 - 삭제 포함(주의)
    Page<User> findAll(Pageable pageable);

    // =========================
    // ✅ Soft Delete "제외" 조회 (deleted_at is null)
    // =========================

    // ✅ 이메일 중복 체크 - 삭제 제외(실무 권장)
    boolean existsByEmailAndDeletedAtIsNull(String email);

    // ✅ 이메일 조회 - 삭제 제외(로그인/조회 실무 권장)
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    // ✅ 유저 전체 페이징 조회 - 삭제 제외
    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    // ✅ 단건 조회 - 삭제 제외
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);
}
