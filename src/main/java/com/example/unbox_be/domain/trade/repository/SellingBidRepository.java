package com.example.unbox_be.domain.trade.repository;

import com.example.unbox_be.domain.trade.entity.SellingBid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @EntityGraph(attributePaths = {"productOption", "productOption.product"})
    Optional<SellingBid> findBySellingId(UUID sellingId);
}