package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductOptionRepository extends JpaRepository<ProductOption, UUID> {
    // 전체조회용 (벌크로 옵션 조회)
    List<ProductOption> findAllByProductIdIn(List<UUID> product);

    // 상세조회용 (1개 상품 옵션 조회)
    List<ProductOption> findAllByProductId(UUID productId);
}
