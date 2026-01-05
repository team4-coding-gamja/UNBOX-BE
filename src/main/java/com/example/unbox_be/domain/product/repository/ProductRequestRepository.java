package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.ProductRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRequestRepository extends JpaRepository<ProductRequest, UUID> {

    Optional<ProductRequest> findByIdAndDeletedAtIsNull(UUID id);
}