package com.example.unbox_be.product.reviews.dto.response;

import com.example.unbox_be.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        private UUID orderId;
        private OrderStatus orderStatus;
        private UserInfo buyer;
        private ProductOptionInfo productOption;
    }

    @Getter
    @Builder
    public static class UserInfo {
        private Long buyerId;
        private String buyerNickname;
    }

    @Getter
    @Builder
    public static class ProductOptionInfo {
        private UUID productOptionId;
        private String productOptionName;
        private ProductInfo product;
    }

    @Getter
    @Builder
    public static class ProductInfo {
        private UUID productId;
        private String productName;
        private String productImageUrl;
    }
}
