package com.example.unbox_user.user.repository;

import com.example.unbox_user.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    Optional<Address> findByUserIdAndIsDefaultTrueAndDeletedAtIsNull(Long userId);
}
