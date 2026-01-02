package com.example.unbox_be.domain.trade.dto.request;

import com.example.unbox_be.domain.trade.entity.SellingStatus;

public record UpdateSellingStatusRequestDto (
    SellingStatus status
) {}
