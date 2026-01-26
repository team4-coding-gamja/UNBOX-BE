package com.example.unbox_product.product.domain.repository;

import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.entity.ProductOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductOptionRepository extends JpaRepository<ProductOption, UUID> {

    // =========================
    // Soft Delete "제외" 조회 (deleted_at is null)
    // =========================

    Optional<ProductOption> findByIdAndDeletedAtIsNull(UUID id);

    // 단건: 특정 상품의 옵션 조회
    List<ProductOption> findAllByProductIdAndDeletedAtIsNull(UUID productId);

    // 벌크: 상품 여러 개의 옵션 한 번에 조회
    List<ProductOption> findAllByProductIdInAndDeletedAtIsNull(List<UUID> productIds);


    // 옵션 중복 체크(삭제 제외, 실무 권장)
    boolean existsByProductAndNameAndDeletedAtIsNull(Product product, String name);

    // =========================
    // 페이징 조회 메서드 추가
    // =========================

    // 특정 상품의 옵션 목록 조회 (페이징)
    Page<ProductOption> findByProductIdAndDeletedAtIsNull(UUID productId, Pageable pageable);

    // 전체 옵션 목록 조회 (페이징)
    Page<ProductOption> findAllByDeletedAtIsNull(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductOption po SET po.deletedAt = CURRENT_TIMESTAMP, po.deletedBy = :deletedBy WHERE po.product.id IN :productIds AND po.deletedAt IS NULL")
    void deleteByProductIdsIn(@Param("productIds") List<UUID> productIds, @Param("deletedBy") String deletedBy);

    @Modifying(clearAutomatically = true) // 중요: 쿼리 실행 후 영속성 컨텍스트 비우기
    @Query("UPDATE ProductOption po SET po.deletedAt = CURRENT_TIMESTAMP, po.deletedBy = :deletedBy WHERE po.product.id = :productId AND po.deletedAt IS NULL")
    void deleteByProductId(@Param("productId") UUID productId, @Param("deletedBy") String deletedBy);
}
