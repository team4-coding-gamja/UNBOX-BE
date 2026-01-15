package com.example.unbox_be.domain.product.product.dto.response;

import com.example.unbox_be.domain.product.product.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminProductCreateResponseDto {

    private UUID brandId;
    private UUID id;
    private String name;
    private String modelNumber;
    private Category category;
    private String productImageUrl;
}
