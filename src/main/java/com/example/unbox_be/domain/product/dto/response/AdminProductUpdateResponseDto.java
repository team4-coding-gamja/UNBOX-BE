package com.example.unbox_be.domain.product.dto.response;

import com.example.unbox_be.domain.product.entity.Category;
import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductUpdateResponseDto {

    private UUID id;
    private UUID brandId;
    private String name;
    private String modelNumber;
    private Category category;
    private String productImageUrl;
}
