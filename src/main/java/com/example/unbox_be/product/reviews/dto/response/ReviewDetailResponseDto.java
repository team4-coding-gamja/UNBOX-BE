package com.example.unbox_be.product.reviews.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewDetailResponseDto {

    private UUID reviewId;
    private String content;
    private Integer rating;
    private String reviewImageUrl;
    private LocalDateTime createdAt;

    private OrderInfo order;

    @Getter
    @Builder
    public static class OrderInfo {
        private UUID id;
        private BigDecimal price;
        private UserInfo buyer;
        private ProductOptionInfo productOption;
    }

    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String nickname;
    }

    @Getter
    @Builder
    public static class ProductOptionInfo {
        private UUID id;
        private String productOptionName;
        private ProductInfo product;
    }

    @Getter
    @Builder
    public static class ProductInfo {
        private UUID id;
        private String name;
        private String productImageUrl;
    }
}
