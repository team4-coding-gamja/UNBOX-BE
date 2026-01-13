package com.example.unbox_be.domain.trade.repository;

import com.example.unbox_be.domain.trade.entity.SellingBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminSellingBidRepository extends JpaRepository<SellingBid, UUID>, AdminSellingBidRepositoryCustom {
    // Basic CRUD provided by JpaRepository
    void deleteAllByProductOptionIdIn(List<UUID> productOptionIds);

    void deleteByProductOptionId(UUID productOptionId);
}
