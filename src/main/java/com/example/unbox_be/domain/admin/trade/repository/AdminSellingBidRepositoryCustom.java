package com.example.unbox_be.domain.admin.trade.repository;

import com.example.unbox_be.domain.admin.trade.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminSellingBidRepositoryCustom {
    Page<SellingBid> findAdminSellingBids(SellingBidSearchCondition condition, Pageable pageable);
}
