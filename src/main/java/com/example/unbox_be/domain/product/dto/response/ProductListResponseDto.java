package com.example.unbox_be.domain.product.dto.response;

import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListResponseDto {

    private UUID id;
    private String name;
    private String modelNumber;
    private Category category;
    private String imageUrl;

    private UUID brandId;
    private String brandName;

    private Integer lowestPrice;
}

