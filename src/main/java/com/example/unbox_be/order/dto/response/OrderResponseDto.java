package com.example.unbox_be.order.dto.response;

import com.example.unbox_be.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    // 1. 주문 기본 정보
    private UUID id;
    private BigDecimal price;
    private OrderStatus status;
    private LocalDateTime createdAt;

    // 2. 상품 정보
    private String brandName;    // 예: Nike
    private String productName;  // 예: Jordan 1 Retro High
    private String size;         // DB의 option 필드 값 (예: "270")
    private String imageUrl;     // 상품 이미지
}