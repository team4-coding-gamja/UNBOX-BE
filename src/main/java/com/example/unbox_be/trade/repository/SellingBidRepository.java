package com.example.unbox_be.trade.repository;

import com.example.unbox_be.trade.entity.SellingBid;
import com.example.unbox_be.trade.entity.SellingStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellingBidRepository extends JpaRepository<SellingBid, UUID> {

    // 특정 사용자의 판매 입찰 내역 조회
    List<SellingBid> findAllBySellerId(Long userId);

    // 특정 상품 옵션ID의 입찰 내역 조회 (가격 낮은 순으로)
    List<SellingBid> findAllByProductOptionIdOrderByPriceAsc(UUID optionId);

    Slice<SellingBid> findBySellerIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
        select sb from SellingBid sb
        where sb.id = :sellingBidId
          and sb.deletedAt is null
            """)
    Optional<SellingBid> findWithDetailsByIdAndDeletedAtIsNull(@Param("sellingBidId") UUID sellingBidId);

    @Query("""
                select sb.productOptionId, min(sb.price)
                from SellingBid sb
                where sb.productOptionId in :optionIds
                  and sb.status = 'LIVE'
                  and sb.deletedAt is null
                group by sb.productOptionId
            """)
    List<Object[]> findLowestPriceByOptionIds(@Param("optionIds") List<UUID> optionIds);

    Optional<SellingBid> findByIdAndDeletedAtIsNull(UUID sellingId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints({
            @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("select sb from SellingBid sb where sb.id = :sellingId and sb.deletedAt is null")
    Optional<SellingBid> findByIdAndDeletedAtIsNullForUpdate(@Param("sellingId") UUID sellingId);

    /**
     * Note: This query cannot directly get product IDs from productOptionId.
     * You'll need to fetch ProductOption data via ProductClient in the service
     * layer
     * to map productOptionId -> productId, then aggregate the lowest prices.
     * 
     * Alternative: Keep this method but it will only work for option-level queries.
     * For product-level queries, implement the logic in the service layer.
     */
    @Query("""
                select sb.productOptionId, min(sb.price)
                from SellingBid sb
                where sb.productOptionId in :optionIds
                  and sb.status = 'LIVE'
                  and sb.deletedAt is null
                group by sb.productOptionId
            """)
    List<Object[]> findLowestPricesByProductOptionIds(@Param("optionIds") List<UUID> optionIds);

    @Query("SELECT MIN(sb.price) " +
            "FROM SellingBid sb " +
            "WHERE sb.productOptionId in :productOptionIds " +
            "AND sb.status = :status " +
            "AND sb.deletedAt IS NULL")
    Integer findLowestPriceByProductOptionIds(
            @Param("productOptionIds") List<UUID> productOptionIds,
            @Param("status") SellingStatus status);

    /**
     * LIVE 상태인 SellingBid를 MATCHED로 변경
     * - 동시 요청 시 단 1건만 성공
     * - 반환값: 변경된 row 수 (1 = 성공, 0 = 실패)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                update SellingBid s
                set s.status = :newStatus
                where s.id = :id
                  and s.status = :expectedStatus
                  and s.deletedAt is null
            """)
    int updateStatusIfMatch(
            @Param("id") UUID id,
            @Param("expectedStatus") SellingStatus expectedStatus,
            @Param("newStatus") SellingStatus newStatus);
}
