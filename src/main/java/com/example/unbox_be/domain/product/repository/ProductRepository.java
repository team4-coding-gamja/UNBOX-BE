package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Override
    @EntityGraph(attributePaths = {"brand"})
    Page<Product> findAll(Pageable pageable);
}
