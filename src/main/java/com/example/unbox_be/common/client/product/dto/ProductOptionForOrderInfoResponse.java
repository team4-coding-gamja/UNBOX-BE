package com.example.unbox_be.common.client.product.dto;

import com.example.unbox_be.product.product.domain.entity.Product;
import com.example.unbox_be.product.product.domain.entity.ProductOption;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductOptionForOrderInfoResponse {
    private UUID productOptionId;
    private String productOptionName;

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;

    private UUID brandId;
    private String brandName;


}
