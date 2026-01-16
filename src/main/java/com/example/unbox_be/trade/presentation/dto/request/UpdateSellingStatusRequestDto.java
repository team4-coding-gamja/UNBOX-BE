package com.example.unbox_be.trade.presentation.dto.request;

import com.example.unbox_be.trade.domain.entity.SellingStatus;

public record UpdateSellingStatusRequestDto (
    SellingStatus status
) {}
