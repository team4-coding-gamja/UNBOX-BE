package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
}
