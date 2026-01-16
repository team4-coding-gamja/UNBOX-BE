package com.example.unbox_be.user.cart.dto.response;

import com.example.unbox_be.trade.domain.entity.SellingStatus;
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
    private UUID cartId;
    private LocalDateTime createdAt;

    private UUID sellingBidId;
    private BigDecimal price;
    private SellingStatus sellingStatus;

    private UUID productOptionId;
    private String productOptionName;

    private String productName;
    private String modelNumber;
    private String productImageUrl;
}
