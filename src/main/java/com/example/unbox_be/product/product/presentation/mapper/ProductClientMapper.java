package com.example.unbox_be.product.product.presentation.mapper;

import com.example.unbox_be.common.client.product.dto.ProductOptionForOrderInfoResponse;
import com.example.unbox_be.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_be.product.product.domain.entity.Brand;
import com.example.unbox_be.product.product.domain.entity.Product;
import com.example.unbox_be.product.product.domain.entity.ProductOption;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductClientMapper {

    default ProductOptionForOrderInfoResponse toProductOptionForOrderInfoResponse(ProductOption productOption) {
        Product product = productOption.getProduct();
        return ProductOptionForOrderInfoResponse.builder()
                .productOptionId(productOption.getId())
                .productOptionName(productOption.getName())

                .productId(product.getId())
                .productName(product.getName())
                .modelNumber(product.getModelNumber())
                .productImageUrl(product.getImageUrl())

                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .build();
    }

    default ProductOptionForSellingBidInfoResponse toProductOptionForSellingBidInfoResponse(ProductOption productOption) {
        Product product = productOption.getProduct();
        Brand brand = product.getBrand();
        return ProductOptionForSellingBidInfoResponse.builder()
                .productOptionId(productOption.getId())
                .productOptionName(productOption.getName())

                .productId(product.getId())
                .productName(product.getName())
                .modelNumber(product.getModelNumber())
                .productImageUrl(product.getImageUrl())

                .brandId(brand.getId())
                .brandName(brand.getName())
                .build();
    }
}
