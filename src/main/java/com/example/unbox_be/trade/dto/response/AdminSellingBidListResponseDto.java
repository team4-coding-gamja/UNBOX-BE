package com.example.unbox_be.trade.dto.response;

import com.example.unbox_be.trade.entity.SellingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSellingBidListResponseDto {
    private UUID sellingBidId;
    private SellingStatus status;
    private BigDecimal price;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;

    private Long sellerId;

    private UUID productOptionId;
    private String productOptionName;

    private UUID productId;
    private String productName;

    private UUID brandId;
    private String brandName;
}
