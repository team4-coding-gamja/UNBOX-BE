package com.example.unbox_trade.trade.domain.repository;

import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE SellingBid s SET s.status = :toStatus WHERE s.id = :id AND s.status = :fromStatus")
  int updateStatusIfReserved(@Param("id") UUID id,
                             @Param("fromStatus") SellingStatus fromStatus,
                             @Param("toStatus") SellingStatus toStatus);

  @Query("SELECT MIN(sb.price) FROM SellingBid sb WHERE sb.productOptionId = :optionId AND sb.status = 'LIVE' AND sb.deletedAt IS NULL")
  Optional<java.math.BigDecimal> findLowestPriceByOptionId(@Param("optionId") UUID optionId);
}
