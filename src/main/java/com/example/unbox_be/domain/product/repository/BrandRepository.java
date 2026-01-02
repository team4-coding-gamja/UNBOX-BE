package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.Optional;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    // 중복 체크
    boolean existsByName (String name);

    // 브랜드 아이디로 조회
    Optional<Brand> findById(UUID id);
}
