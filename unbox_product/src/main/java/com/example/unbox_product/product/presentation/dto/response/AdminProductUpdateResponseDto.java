package com.example.unbox_product.product.presentation.dto.response;

import com.example.unbox_product.product.domain.entity.Category;
import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductUpdateResponseDto {

    private UUID productId;
    private UUID brandId;
    private String productName;
    private String modelNumber;
    private Category category;
    private String productImageUrl;
}
