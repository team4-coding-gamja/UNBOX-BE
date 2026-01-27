package com.example.unbox_user.user.repository;

import com.example.unbox_user.user.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findAllByUserId(Long userId);
    
    Optional<Account> findByIdAndUserId(UUID id, Long userId);

    // 해당 유저의 대표 계좌가 있는지 확인
    boolean existsByUserIdAndIsDefaultTrue(Long userId);

    // 해당 유저의 대표 계좌 조회
    Optional<Account> findByUserIdAndIsDefaultTrue(Long userId);
}
