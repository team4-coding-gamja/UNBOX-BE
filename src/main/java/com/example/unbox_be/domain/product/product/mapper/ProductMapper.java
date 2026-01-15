package com.example.unbox_be.domain.product.product.mapper;

import com.example.unbox_be.domain.product.product.dto.response.ProductDetailResponseDto;
import com.example.unbox_be.domain.product.product.dto.response.ProductListResponseDto;
import com.example.unbox_be.domain.product.product.dto.response.ProductOptionListResponseDto;
import com.example.unbox_be.domain.product.product.entity.Product;
import com.example.unbox_be.domain.product.product.entity.ProductOption;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    default ProductListResponseDto toProductListDto(Product product, Integer lowestPrice) {
        return ProductListResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .productImageUrl(product.getProductImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .lowestPrice(lowestPrice)
                .build();
    }

    default ProductDetailResponseDto toProductDetailDto(Product product, Integer lowestPrice) {

        // 평균 리뷰 계산
        double avg = 0.0;
        if (product.getReviewCount() > 0){
            avg = (double) product.getTotalScore() / product.getReviewCount();
            avg = Math.round(avg * 10) / 10.0;
        }

        return ProductDetailResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .productImageUrl(product.getProductImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .reviewCount(product.getReviewCount())
                .averageRating(avg)
                .lowestPrice(lowestPrice)
                .build();
    }

    default ProductOptionListResponseDto toProductOptionListDto(ProductOption option, Integer lowestPrice) {
        return ProductOptionListResponseDto.builder()
                .id(option.getId())
                .productOptionName(option.getOption())
                .lowestPrice(lowestPrice)
                .build();
    }

    /* =========================
       목록 조회 (최저가 포함)
       ========================= */

    default ProductListResponseDto toProductListResponseDto(
            Product product,
            Integer lowestPrice
    ) {
        return ProductListResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .productImageUrl(product.getProductImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .lowestPrice(lowestPrice)
                .build();
    }
}
