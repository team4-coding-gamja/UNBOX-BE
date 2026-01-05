package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductOptionRepository extends JpaRepository<ProductOption, UUID> {
    // ✅ 벌크 옵션 조회 (목록용)
    List<ProductOption> findAllByProductIdIn(List<UUID> productIds);

    // ✅ 상세 옵션 조회
    List<ProductOption> findAllByProductId(UUID productId);

    boolean existsByProductAndOption(Product product, String option);
}
