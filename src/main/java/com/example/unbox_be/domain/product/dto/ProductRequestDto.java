package com.example.unbox_be.domain.product.dto;

import com.example.unbox_be.domain.product.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {
    private String brandName;
    private String name;
    private String modelNumber;
    private Category category;
    private String imageUrl;
    private List<String> options;
}
