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
@AllArgsConstructor
@NoArgsConstructor
public class SellingBidDetailResponseDto {
    private UUID id;
    private SellingStatus status;
    private BigDecimal price;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;

    private Long sellerId;

    private String productName;
    private String modelNumber;
    private String productImageUrl;
    private String productOptionName;
}
