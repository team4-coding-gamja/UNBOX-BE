package com.example.unbox_trade.trade.domain.repository;

import com.example.unbox_trade.trade.domain.entity.SellingBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AdminSellingBidRepository extends JpaRepository<SellingBid, UUID>, AdminSellingBidRepositoryCustom {
    // Basic CRUD provided by JpaRepository
    void deleteAllByProductOptionIdIn(List<UUID> productOptionIds);

    void deleteByProductOptionId(UUID productOptionId);

    // ✅ [Bulk Update] 여러 옵션 ID에 해당하는 입찰 내역 일괄 Soft Delete
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SellingBid sb SET sb.deletedAt = CURRENT_TIMESTAMP, sb.deletedBy = :deletedBy WHERE sb.productOptionId IN :optionIds AND sb.deletedAt IS NULL")
    void softDeleteByOptionIds(@Param("optionIds") List<UUID> optionIds, @Param("deletedBy") String deletedBy);

    // ✅ [Bulk Update] 단건 옵션 ID에 해당하는 입찰 내역 일괄 Soft Delete
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SellingBid sb SET sb.deletedAt = CURRENT_TIMESTAMP, sb.deletedBy = :deletedBy WHERE sb.productOptionId = :optionId AND sb.deletedAt IS NULL")
    void softDeleteByOptionId(@Param("optionId") UUID optionId, @Param("deletedBy") String deletedBy);
}
