package com.example.unbox_be.domain.user.repository;

import com.example.unbox_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    //email을 받아 DB 테이블에서 회원을 조회하는 메소드 작성
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Page<User> findAll(Pageable pageable);

}
