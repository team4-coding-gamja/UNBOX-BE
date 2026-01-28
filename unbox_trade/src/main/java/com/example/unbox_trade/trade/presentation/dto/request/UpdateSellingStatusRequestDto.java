package com.example.unbox_trade.trade.presentation.dto.request;

import com.example.unbox_trade.trade.domain.entity.SellingStatus;

public record UpdateSellingStatusRequestDto (
    SellingStatus status,
    String updatedBy
) {}
