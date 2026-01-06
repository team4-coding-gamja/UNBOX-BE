package com.example.unbox_be.domain.reviews.dto.response;

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

    private UUID id;
    private String content;
    private Integer rating;
    private String imageUrl;
    private LocalDateTime createdAt;

    private OrderInfo order;
    private ProductInfo product;
    private UserInfo user;

    @Getter
    @Builder
    public static class OrderInfo {
        private UUID id;
        private BigDecimal price;
    }

    @Getter
    @Builder
    public static class ProductInfo {
        private UUID id;
        private String name;
    }

    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String nickname;
    }


}
