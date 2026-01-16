package com.example.unbox_be.trade.domain.repository;

import com.example.unbox_be.trade.presentation.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminSellingBidRepositoryCustom {
    Page<SellingBid> findAdminSellingBids(SellingBidSearchCondition condition, Pageable pageable);
}
