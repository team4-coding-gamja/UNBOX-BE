package com.example.unbox_be.domain.cart.dto.response;

import com.example.unbox_be.domain.trade.entity.SellingStatus;
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
public class CartListResponseDto {
    private Long cartId;
    private LocalDateTime createdAt;

    private UUID sellingBidId;
    private BigDecimal price;
    private SellingStatus sellingStatus;

    private UUID productOptionId;
    private String productOptionName;

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String imageUrl;
}
