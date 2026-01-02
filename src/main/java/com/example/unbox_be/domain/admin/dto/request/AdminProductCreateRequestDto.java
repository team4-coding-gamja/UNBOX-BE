package com.example.unbox_be.domain.admin.dto.request;

import com.example.unbox_be.domain.product.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductCreateRequestDto {

    private UUID brandId;
    private String name;
    private String modelNumber;
    private Category category;
    private String imageUrl;
}
