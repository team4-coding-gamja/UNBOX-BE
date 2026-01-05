package com.example.unbox_be.domain.admin.order.repository;

import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.unbox_be.domain.order.entity.QOrder.order;
import static com.example.unbox_be.domain.product.entity.QBrand.brand;
import static com.example.unbox_be.domain.product.entity.QProduct.product;
import static com.example.unbox_be.domain.product.entity.QProductOption.productOption;

@Repository
@RequiredArgsConstructor
public class OrderAdminRepositoryCustomImpl implements OrderAdminRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> findAdminOrders(OrderSearchCondition condition, Pageable pageable) {
        List<Order> content = queryFactory
                .selectFrom(order)
                .leftJoin(order.productOption, productOption).fetchJoin()
                .leftJoin(productOption.product, product).fetchJoin()
                .leftJoin(product.brand, brand).fetchJoin()
                .where(
                        statusEq(condition.getStatus()),
                        productNameContains(condition.getProductName()),
                        brandNameContains(condition.getBrandName()),
                        buyerNameContains(condition.getBuyerName()),
                        periodBetween(condition.getStartDate(), condition.getEndDate())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(order.createdAt.desc()) // 최신순 기본 정렬
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(order.count())
                .from(order)
                .where(
                        statusEq(condition.getStatus()),
                        productNameContains(condition.getProductName()),
                        brandNameContains(condition.getBrandName()),
                        buyerNameContains(condition.getBuyerName()),
                        periodBetween(condition.getStartDate(), condition.getEndDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }

    private BooleanExpression productNameContains(String productName) {
        return productName != null ? product.name.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression brandNameContains(String brandName) {
        return brandName != null ? brand.name.containsIgnoreCase(brandName) : null;
    }

    private BooleanExpression buyerNameContains(String buyerName) {
        // Buyer fetch join logic might be needed if buyer is not joined.
        // Assuming buyer relation exists. If lazy loaded, might trigger N+1 if not careful,
        // but here we are just filtering.
        // Wait, Order -> Buyer relationship exists.
        return buyerName != null ? order.buyer.nickname.containsIgnoreCase(buyerName) : null;
    }

    private BooleanExpression periodBetween(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        if (startDate != null && endDate != null) {
            return order.createdAt.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        }
        if (startDate != null) {
            return order.createdAt.goe(startDate.atStartOfDay());
        }
        return order.createdAt.lt(endDate.plusDays(1).atStartOfDay());
    }
}
