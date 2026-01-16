package com.example.unbox_be.product.reviews.entity;

import com.example.unbox_be.order.entity.OrderStatus;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewProductSnapshot {

    private Long buyerId;
    private String buyerNickname;
    private OrderStatus orderStatus;
    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;
    private UUID productOptionId;
    private String productOptionName;
    private String brandName;
}
