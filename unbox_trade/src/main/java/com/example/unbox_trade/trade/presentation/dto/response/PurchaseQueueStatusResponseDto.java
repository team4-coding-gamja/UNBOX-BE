package com.example.unbox_trade.trade.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PurchaseQueueStatusResponseDto {
    private long position;
    private long aheadCount;
}
