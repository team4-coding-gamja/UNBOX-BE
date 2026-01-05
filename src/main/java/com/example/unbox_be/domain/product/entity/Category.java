package com.example.unbox_be.domain.product.entity;

public enum Category {
    SHOES;
    public static Category fromNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return Category.valueOf(value.trim().toUpperCase());
    }
}
