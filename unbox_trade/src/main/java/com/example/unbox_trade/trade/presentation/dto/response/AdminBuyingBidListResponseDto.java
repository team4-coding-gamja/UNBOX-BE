package com.example.unbox_trade.trade.presentation.dto.response;

import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBuyingBidListResponseDto {
    private UUID buyingBidId;
    private String productName;
    private String brandName;
    private String size;
    private BigDecimal price;
    private BuyingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
}
