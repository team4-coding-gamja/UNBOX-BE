package com.example.unbox_be.domain.product.product.repository;

import com.example.unbox_be.domain.product.product.entity.Product;
import com.example.unbox_be.domain.product.product.entity.ProductOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductOptionRepository extends JpaRepository<ProductOption, UUID> {

    // =========================
    // ✅ Soft Delete "제외" 조회 (deleted_at is null)
    // =========================

    Optional<ProductOption> findByIdAndDeletedAtIsNull(UUID id);

    // ✅ 단건: 특정 상품의 옵션 조회
    List<ProductOption> findAllByProductIdAndDeletedAtIsNull(UUID productId);

    // ✅ 벌크: 상품 여러 개의 옵션 한 번에 조회
    List<ProductOption> findAllByProductIdInAndDeletedAtIsNull(List<UUID> productIds);


    // ✅ 옵션 중복 체크(삭제 제외, 실무 권장)
    boolean existsByProductAndOptionAndDeletedAtIsNull(Product product, String option);

    // =========================
    // ✅ 페이징 조회 메서드 추가
    // =========================

    // 1️⃣ 특정 상품의 옵션 목록 조회 (페이징)
    Page<ProductOption> findByProductIdAndDeletedAtIsNull(UUID productId, Pageable pageable);

    // 2️⃣ 전체 옵션 목록 조회 (페이징)
    Page<ProductOption> findAllByDeletedAtIsNull(Pageable pageable);
}
