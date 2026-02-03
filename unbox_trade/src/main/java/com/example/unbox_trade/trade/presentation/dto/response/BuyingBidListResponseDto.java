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
public class BuyingBidListResponseDto {
    private UUID buyingId;
    private String productName;
    private String productOptionName;
    private String productImageUrl;
    private BigDecimal price;
    private BuyingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
}
