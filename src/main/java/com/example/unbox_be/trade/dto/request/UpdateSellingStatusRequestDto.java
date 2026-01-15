package com.example.unbox_be.trade.dto.request;

import com.example.unbox_be.trade.entity.SellingStatus;

public record UpdateSellingStatusRequestDto (
    SellingStatus status
) {}
