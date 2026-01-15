package com.example.unbox_be.order.repository;

import com.example.unbox_be.order.dto.OrderSearchCondition;
import com.example.unbox_be.order.entity.Order;
import com.example.unbox_be.order.entity.OrderStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.unbox_be.order.entity.QOrder.order;

@Repository
@RequiredArgsConstructor
public class AdminOrderRepositoryCustomImpl implements AdminOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> findAdminOrders(OrderSearchCondition condition, Pageable pageable) {
        List<Order> content = queryFactory
                .selectFrom(order)
                .leftJoin(order.buyer).fetchJoin()
                .leftJoin(order.seller).fetchJoin() // seller도 같이 fetchJoin 하는 것이 좋음 (상세 정보용)
                .where(
                        statusEq(condition.getStatus()),
                        productNameContains(condition.getProductName()),
                        brandNameContains(condition.getBrandName()),
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
                        periodBetween(condition.getStartDate(), condition.getEndDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }

    private BooleanExpression productNameContains(String productName) {
        return productName != null ? order.productName.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression brandNameContains(String brandName) {
        return brandName != null ? order.brandName.containsIgnoreCase(brandName) : null;
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
