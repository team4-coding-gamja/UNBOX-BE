package com.example.unbox_be.product.product.presentation.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchCondition {

    private UUID brandId;
    private String category; // 요청은 String으로 받는게 편함 (enum 변환은 service에서)
    private String keyword;
}
