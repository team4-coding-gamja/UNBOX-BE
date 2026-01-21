package com.example.unbox_product.product.presentation.mapper;

import com.example.unbox_product.product.presentation.dto.redis.ProductRedisDto;
import com.example.unbox_product.product.presentation.dto.response.ProductDetailResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductOptionListResponseDto;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.entity.ProductOption;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    default ProductDetailResponseDto toProductDetailResponseDto(ProductRedisDto infoDto, BigDecimal lowestPrice) {
        // 평점 계산 로직 (RedisDto에 있는 값 활용)
        double avg = 0.0;
        if (infoDto.getReviewCount() > 0) {
            avg = (double) infoDto.getTotalScore() / infoDto.getReviewCount();
            avg = Math.round(avg * 10) / 10.0;
        }

        return ProductDetailResponseDto.builder()
                .productId(infoDto.getProductId())
                .productName(infoDto.getName())
                .modelNumber(infoDto.getModelNumber())
                .productImageUrl(infoDto.getImageUrl())
                // 🚨 [수정] 이제 DTO에 값이 있으니 채워넣기 가능!
                .brandId(infoDto.getBrandId())
                .brandName(infoDto.getBrandName())
                .category(infoDto.getCategory())
                .reviewCount(infoDto.getReviewCount())
                .averageRating(avg)
                .lowestPrice(lowestPrice)
                // .options(...)
                // 필요하다면 여기서 옵션도 매핑
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

    default ProductOptionListResponseDto toProductOptionListDtoFromRedis(ProductRedisDto.ProductOptionDto redisOption) {
        return ProductOptionListResponseDto.builder()
                .productOptionId(redisOption.getOptionId())
                .productOptionName(redisOption.getOptionName())
                .build();
    }
}
