package com.example.unbox_be.domain.product.dto;

import com.example.unbox_be.domain.product.entity.Category;
import lombok.Data;

@Data
public class ProductSearchCondition {
    private String keyword;
    private String brandName;
    private Category category;
}
