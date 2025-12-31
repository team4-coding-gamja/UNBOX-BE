package com.example.unbox_be.domain.order.mapper;

import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponseDto toResponseDto(Order order) {
        // 1. 연관된 엔티티 꺼내기 (N+1 방지를 위해 추후 fetch join 고려)
        ProductOption productOption = order.getProductOption();
        Product product = productOption.getProduct(); // ProductOption 안의 Product

        // 2. 브랜드 이름 안전하게 꺼내기 (null 체크)
        String brandName = (product.getBrand() != null) ? product.getBrand().getName() : "";

        // 3. DTO 빌드
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .price(order.getPrice().longValue()) // Integer -> Long
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                // 상품 관련 매핑
                .brandName(brandName)
                .productName(product.getName())
                .size(productOption.getOption()) // String 타입의 상품 옵션을 그대로 가져옴
                .imageUrl(product.getImageUrl())
                .build();
    }
}