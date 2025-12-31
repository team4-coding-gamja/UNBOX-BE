package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.dto.ProductSearchCondition;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.Category;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

// Q클래스 import (빌드 후 빨간줄 사라짐)
import static com.example.unbox_be.domain.product.entity.QProduct.product;
import static com.example.unbox_be.domain.product.entity.QBrand.brand;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Product> search(ProductSearchCondition condition, Pageable pageable) {

        // 1. 컨텐츠 조회
        List<Product> content = queryFactory
                .selectFrom(product)
                .leftJoin(product.brand, brand).fetchJoin() // 성능 최적화
                .where(
                        keywordContains(condition.getKeyword()),
                        brandNameEq(condition.getBrandName()),
                        categoryEq(condition.getCategory())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. Count 쿼리 (최적화)
        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .leftJoin(product.brand, brand)
                .where(
                        keywordContains(condition.getKeyword()),
                        brandNameEq(condition.getBrandName()),
                        categoryEq(condition.getCategory())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // --- 동적 쿼리 조건 ---
    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword)
                ? product.name.contains(keyword).or(product.modelNumber.contains(keyword))
                : null;
    }

    private BooleanExpression brandNameEq(String brandName) {
        return StringUtils.hasText(brandName) ? brand.name.eq(brandName) : null;
    }

    private BooleanExpression categoryEq(Category category) {
        return category != null ? product.category.eq(category) : null;
    }
}