package com.example.unbox_be.domain.order.mapper;

import com.example.unbox_be.domain.order.dto.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    /**
     * 목록 조회용 DTO 변환 (Flat Structure)
     */
    public OrderResponseDto toResponseDto(Order order) {
        // 1. 안전하게 연관 엔티티 추출 (공통 메서드 사용)
        ProductOption productOption = getProductOptionOrThrow(order);
        Product product = getProductOrThrow(productOption);
        String brandName = getBrandNameSafe(product);

        // 2. DTO 빌드
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .price(order.getPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                // 상품 관련 매핑
                .brandName(brandName)
                .productName(product.getName())
                .size(productOption.getOption()) // String 타입의 옵션값
                .imageUrl(product.getImageUrl())
                .build();
    }

    /**
     * 상세 조회용 DTO 변환 (Nested Structure)
     * - 프론트엔드 요구사항에 맞춰 계층형 구조로 반환
     */
    public OrderDetailResponseDto toDetailResponseDto(Order order) {
        // 1. 안전하게 연관 엔티티 추출 (공통 메서드 사용)
        ProductOption productOption = getProductOptionOrThrow(order);
        Product product = getProductOrThrow(productOption);
        String brandName = getBrandNameSafe(product);

        // 2. 계층형 DTO 빌드
        return OrderDetailResponseDto.builder()
                // 기본 정보
                .orderId(order.getId())
                .price(order.getPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                // 배송 정보
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress() + " " + order.getReceiverZipCode())
                .receiverZipCode(order.getReceiverZipCode())
                .trackingNumber(order.getTrackingNumber())
                // 상품 옵션 정보 (Nested)
                .productOption(OrderDetailResponseDto.ProductOptionInfo.builder()
                        .id(productOption.getId())
                        .size(productOption.getOption())
                        .build())
                // 상품 정보 (Nested)
                .product(OrderDetailResponseDto.ProductInfo.builder()
                        .id(product.getId())
                        .brandName(brandName)
                        .name(product.getName())
                        .modelNumber(product.getModelNumber())
                        .imageUrl(product.getImageUrl())
                        .build())
                .build();
    }

    // --- 공통 추출 메서드 (Private Helper Methods) ---

    /**
     * 주문에서 ProductOption을 안전하게 꺼냄 (없으면 예외 발생)
     */
    private ProductOption getProductOptionOrThrow(Order order) {
        if (order.getProductOption() == null) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return order.getProductOption();
    }

    /**
     * ProductOption에서 Product를 안전하게 꺼냄 (없으면 예외 발생)
     */
    private Product getProductOrThrow(ProductOption option) {
        if (option.getProduct() == null) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return option.getProduct();
    }

    /**
     * Product에서 Brand 이름을 안전하게 꺼냄 (브랜드가 없으면 빈 문자열 반환)
     */
    private String getBrandNameSafe(Product product) {
        Brand brand = product.getBrand();
        return (brand != null) ? brand.getName() : "";
    }
}