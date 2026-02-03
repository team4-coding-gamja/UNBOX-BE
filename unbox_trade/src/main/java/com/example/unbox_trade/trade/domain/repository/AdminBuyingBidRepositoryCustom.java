package com.example.unbox_trade.trade.domain.repository;

import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminBuyingBidRepositoryCustom {
    Page<BuyingBid> findAdminBuyingBids(BuyingBidSearchCondition condition, Pageable pageable);
}
