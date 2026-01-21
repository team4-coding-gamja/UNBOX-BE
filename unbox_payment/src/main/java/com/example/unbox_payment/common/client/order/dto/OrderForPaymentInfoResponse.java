package com.example.unbox_payment.common.client.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderForPaymentInfoResponse {
    private UUID orderId;
    private String status;
    private BigDecimal price;
    private UUID sellingBidId;

    // 구매자/판매자 ID
    private Long buyerId;
    private Long sellerId;
}
