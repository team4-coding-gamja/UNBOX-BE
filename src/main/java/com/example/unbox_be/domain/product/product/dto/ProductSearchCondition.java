package com.example.unbox_be.domain.product.product.dto;

import com.example.unbox_be.domain.product.product.entity.Category;
import lombok.Data;

@Data
public class ProductSearchCondition {
    private String keyword;
    private String brandName;
    private Category category;
}
