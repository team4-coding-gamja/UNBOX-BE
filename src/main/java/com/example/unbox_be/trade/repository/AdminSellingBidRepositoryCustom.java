package com.example.unbox_be.trade.repository;

import com.example.unbox_be.trade.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.trade.entity.SellingBid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminSellingBidRepositoryCustom {
    Page<SellingBid> findAdminSellingBids(SellingBidSearchCondition condition, Pageable pageable);
}
