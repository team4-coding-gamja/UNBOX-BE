package com.example.unbox_be.domain.trade.trade.dto.request;

import com.example.unbox_be.domain.trade.trade.entity.SellingStatus;

public record UpdateSellingStatusRequestDto (
    SellingStatus status
) {}
