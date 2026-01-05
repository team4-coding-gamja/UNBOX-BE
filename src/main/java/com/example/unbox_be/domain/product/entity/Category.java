package com.example.unbox_be.domain.product.entity;

public enum Category {
    SHOES;
    public static Category fromNullable(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Category.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
