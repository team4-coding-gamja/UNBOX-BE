package com.example.unbox_be.domain.order.dto;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OrderResponseDto {
    private UUID orderId;
    private String productName;   // 상품명 (Product 엔티티의 name)
    private String productOption; // 옵션명 (ProductOption 엔티티의 option)
    private BigDecimal price;
    private OrderStatus status;
    private LocalDateTime createdAt;

    // static method 'from' 제거 -> Mapper가 담당
}