package com.example.unbox_be.trade.presentation.dto.response;

import com.example.unbox_be.trade.domain.entity.SellingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SellingBidResponseDto {
    private UUID sellingBidId;
    private SellingStatus status;
    private Long sellerId;
    private Integer price;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;

    // 연관된 상품 정보
    private String productName;
    private String modelNumber;
    private String productImageUrl;

    private String productOptionName;
}