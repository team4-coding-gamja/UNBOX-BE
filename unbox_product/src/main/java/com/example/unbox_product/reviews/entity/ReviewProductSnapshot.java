package com.example.unbox_product.reviews.entity;

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
    private String orderStatus;
    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;
    private UUID productOptionId;
    private String productOptionName;
    private String brandName;
}
