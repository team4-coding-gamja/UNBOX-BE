package com.example.unbox_be.domain.product.mapper;

import com.example.unbox_be.domain.product.dto.response.ProductDetailResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductListResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductOptionListResponseDto;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
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
                .imageUrl(product.getImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .lowestPrice(lowestPrice)
                .build();
    }

    default ProductDetailResponseDto toProductDetailDto(Product product, Integer lowestPrice) {
        return ProductDetailResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
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
}
