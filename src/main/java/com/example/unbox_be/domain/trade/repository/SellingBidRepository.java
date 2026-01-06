package com.example.unbox_be.domain.trade.repository;

import com.example.unbox_be.domain.trade.dto.response.ProductSizePriceResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellingBidRepository extends JpaRepository<SellingBid, UUID> {

    // 특정 사용자의 판매 입찰 내역 조회
    List<SellingBid> findAllByUserId(Long userId);

    // 특정 상품 옵션ID의 입찰 내역 조회 (가격 낮은 순으로)
    List<SellingBid> findAllByProductOptionIdOrderByPriceAsc(UUID optionId);

    @EntityGraph(attributePaths = {"productOption", "productOption.product"})
    Slice<SellingBid> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
    select sb from SellingBid sb
    join fetch sb.productOption po
    join fetch po.product p
    where sb.id = :sellingBidId
      and sb.deletedAt is null
""")
    Optional<SellingBid> findWithDetailsByIdAndDeletedAtIsNull(@Param("sellingBidId") UUID sellingBidId);

    @Query("SELECT new com.example.unbox_be.domain.trade.dto.response.ProductSizePriceResponseDto(" +
            "po.option, MIN(s.price)) " +  // ✅ Enum → String 변환
            "FROM SellingBid s " +
            "JOIN s.productOption po " +
            "WHERE po.product.id = :productId " +
            "AND s.status = :status " +
            "GROUP BY po.option " +        // ✅ SELECT 일반 필드와 동일하게
            "ORDER BY po.option ASC")
    List<ProductSizePriceResponseDto> findLowestPriceByProductId(
            @Param("productId") UUID productId,
            @Param("status") SellingStatus status
    );

    @Query("""
        select sb.productOption.id, min(sb.price)
        from SellingBid sb
        where sb.productOption.id in :optionIds
        group by sb.productOption.id
    """)
    List<Object[]> findLowestPriceByOptionIds(List<UUID> optionIds);

    Optional<SellingBid> findByIdAndDeletedAtIsNull(UUID sellingId);

    // ✅ 상품 ID 목록에 해당하는 최저가 조회 (상품별 최저가)
    @Query("""
        select po.product.id, min(sb.price)
        from SellingBid sb
        join sb.productOption po
        where po.product.id in :productIds
          and sb.status = 'IN_PROGRESS'
          and sb.deletedAt is null
        group by po.product.id
    """)
    List<Object[]> findLowestPricesByProductIds(@Param("productIds") List<UUID> productIds);
}
