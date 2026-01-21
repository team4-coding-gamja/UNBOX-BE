package com.example.unbox_product.reviews.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewProductSnapshot {

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "model_number")
    private String modelNumber;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(name = "product_option_id")
    private UUID productOptionId;

    @Column(name = "product_option_name")
    private String productOptionName;

    @Column(name = "brand_name")
    private String brandName;
}
