package com.example.unbox_trade.trade.domain.repository;

import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AdminBuyingBidRepository extends JpaRepository<BuyingBid, UUID>, AdminBuyingBidRepositoryCustom {

    // [Bulk Update] 여러 옵션 ID에 해당하는 구매 입찰 내역 일괄 Soft Delete
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BuyingBid bb SET bb.deletedAt = CURRENT_TIMESTAMP, bb.deletedBy = :deletedBy WHERE bb.productOptionId IN :optionIds AND bb.deletedAt IS NULL")
    void softDeleteByOptionIds(@Param("optionIds") List<UUID> optionIds, @Param("deletedBy") String deletedBy);

    // [Bulk Update] 단건 옵션 ID에 해당하는 구매 입찰 내역 일괄 Soft Delete
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BuyingBid bb SET bb.deletedAt = CURRENT_TIMESTAMP, bb.deletedBy = :deletedBy WHERE bb.productOptionId = :optionId AND bb.deletedAt IS NULL")
    void softDeleteByOptionId(@Param("optionId") UUID optionId, @Param("deletedBy") String deletedBy);
}
