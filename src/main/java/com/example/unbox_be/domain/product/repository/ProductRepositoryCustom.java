package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.dto.ProductSearchCondition;
import com.example.unbox_be.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {
    Page<Product> search(ProductSearchCondition condition, Pageable pageable);
}
