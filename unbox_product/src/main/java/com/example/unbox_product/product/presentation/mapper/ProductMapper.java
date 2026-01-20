package com.example.unbox_product.product.presentation.mapper;

import com.example.unbox_product.product.presentation.dto.response.ProductDetailResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductOptionListResponseDto;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.entity.ProductOption;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    default ProductDetailResponseDto toProductDetailDto(Product product) {

        // 평균 리뷰 계산
        double avg = 0.0;
        if (product.getReviewCount() > 0){
            avg = (double) product.getTotalScore() / product.getReviewCount();
            avg = Math.round(avg * 10) / 10.0;
        }

        return ProductDetailResponseDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .productImageUrl(product.getImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .reviewCount(product.getReviewCount())
                .averageRating(avg)
                .build();
    }

    default ProductOptionListResponseDto toProductOptionListDto(ProductOption option) {
        return ProductOptionListResponseDto.builder()
                .productOptionId(option.getId())
                .productOptionName(option.getName())
                .build();
    }

    default ProductListResponseDto toProductListResponseDto(Product product) {
        return ProductListResponseDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .productImageUrl(product.getImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .build();
    }
}
