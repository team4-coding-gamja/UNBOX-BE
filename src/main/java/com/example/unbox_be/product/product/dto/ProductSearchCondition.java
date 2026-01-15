package com.example.unbox_be.product.product.dto;

import com.example.unbox_be.product.product.entity.Category;
import lombok.Data;

@Data
public class ProductSearchCondition {
    private String keyword;
    private String brandName;
    private Category category;
}
