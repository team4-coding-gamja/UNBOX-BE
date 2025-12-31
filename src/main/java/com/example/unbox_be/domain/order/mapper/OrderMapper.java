package com.example.unbox_be.domain.order.mapper;

import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    /**
     * Entity -> Response DTO 변환
     * - DTO가 Entity를 직접 의존하지 않도록 Mapper가 중간 다리 역할을 수행
     */
    public OrderResponseDto toResponseDto(Order order) {
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .productName(order.getProductOption().getProduct().getName())
                .productOption(order.getProductOption().getOption())
                .price(order.getPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}