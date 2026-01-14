package com.example.unbox_be.domain.product.dto.response;

import com.example.unbox_be.domain.product.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminProductListResponseDto {

    private UUID id;
    private String name;
    private String modelNumber;
    private Category category;
    private String productproductImageUrl;

    private UUID brandId;
    private String brandName;
}
