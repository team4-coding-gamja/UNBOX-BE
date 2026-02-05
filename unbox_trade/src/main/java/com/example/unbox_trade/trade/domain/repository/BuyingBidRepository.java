package com.example.unbox_trade.trade.domain.repository;

import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuyingBidRepository extends JpaRepository<BuyingBid, UUID> {

  Slice<BuyingBid> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

  Optional<BuyingBid> findByIdAndDeletedAtIsNull(UUID buyingBidId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT b FROM BuyingBid b WHERE b.id = :id AND b.deletedAt IS NULL")
  Optional<BuyingBid> findByIdWithLock(@Param("id") UUID id);

  @Query("SELECT MAX(bb.price) FROM BuyingBid bb WHERE bb.productOptionId = :optionId AND bb.status = 'LIVE' AND bb.deletedAt IS NULL")
  Optional<BigDecimal> findHighestPriceByOptionId(@Param("optionId") UUID optionId);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE BuyingBid b SET b.status = :toStatus WHERE b.id = :id AND b.status = :fromStatus")
  int updateStatusIfReserved(@Param("id") UUID id,
      @Param("fromStatus") BuyingStatus fromStatus,
      @Param("toStatus") BuyingStatus toStatus);

  @Query("""
          select bb.productOptionId, max(bb.price)
          from BuyingBid bb
          where bb.productOptionId in :optionIds
            and bb.status = 'LIVE'
            and bb.deletedAt is null
          group by bb.productOptionId
      """)
  List<Object[]> findHighestPricesByProductOptionIds(@Param("optionIds") java.util.List<UUID> optionIds);
}
