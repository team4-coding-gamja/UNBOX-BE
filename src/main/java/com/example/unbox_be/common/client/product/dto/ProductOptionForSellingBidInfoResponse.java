package com.example.unbox_be.common.client.product.dto;

import com.example.unbox_be.product.product.entity.Brand;
import com.example.unbox_be.product.product.entity.Product;
import com.example.unbox_be.product.product.entity.ProductOption;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductOptionForSellingBidInfoResponse {

    private UUID id;
    private String productOptionName; // Size or option name
    private UUID productId; // Product ID
    private String productName; // Product name
    private String modelNumber;
    private String productImageUrl; // Product image URL
    private UUID brandId;
    private String brandName;

    public static ProductOptionForSellingBidInfoResponse from(ProductOption productOption) {
        Product product = productOption.getProduct();
        Brand brand = product.getBrand();
        return ProductOptionForSellingBidInfoResponse.builder()
                .id(productOption.getId())
                .productOptionName(productOption.getOption())

                .productId(product.getId())
                .productName(product.getName())
                .modelNumber(product.getModelNumber())
                .productImageUrl(product.getProductImageUrl())

                .brandId(brand.getId())
                .brandName(brand.getName())
                .build();
    }
}