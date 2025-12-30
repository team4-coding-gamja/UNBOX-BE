package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProductOptionRepository extends JpaRepository<ProductOption, UUID> {
}
