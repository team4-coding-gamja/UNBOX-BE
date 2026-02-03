package com.example.unbox_trade.trade.presentation.dto.response;

import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuyingBidDetailResponseDto {
    private UUID buyingId;
    private Long buyerId;

    // Product Info
    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;
    private String productOptionName;
    private String brandName;

    private BigDecimal price;
    private BuyingStatus status;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
}
