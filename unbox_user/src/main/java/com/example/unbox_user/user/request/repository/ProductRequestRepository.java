package com.example.unbox_user.user.request.repository;

import com.example.unbox_user.user.request.entity.ProductRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRequestRepository extends JpaRepository<ProductRequest, UUID> {

    Optional<ProductRequest> findByIdAndDeletedAtIsNull(UUID id);
}