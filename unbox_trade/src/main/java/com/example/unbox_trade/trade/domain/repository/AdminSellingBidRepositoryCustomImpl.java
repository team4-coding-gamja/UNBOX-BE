package com.example.unbox_trade.trade.domain.repository;

import com.example.unbox_trade.trade.presentation.dto.request.SellingBidSearchCondition;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.example.unbox_trade.trade.domain.entity.QSellingBid.sellingBid;

@Repository
@RequiredArgsConstructor
public class AdminSellingBidRepositoryCustomImpl implements AdminSellingBidRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<SellingBid> findAdminSellingBids(SellingBidSearchCondition condition, Pageable pageable) {

        List<SellingBid> content = queryFactory
                .selectFrom(sellingBid)
                .where(
                        statusEq(condition.getStatus()),
                        productNameContains(condition.getProductName()),
                        brandNameContains(condition.getBrandName()), 
                        periodBetween(condition.getStartDate(), condition.getEndDate())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(sellingBid.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(sellingBid.count())
                .from(sellingBid)
                .where(
                        statusEq(condition.getStatus()),
                        productNameContains(condition.getProductName()),
                        brandNameContains(condition.getBrandName()),
                        periodBetween(condition.getStartDate(), condition.getEndDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression statusEq(SellingStatus status) {
        return status != null ? sellingBid.status.eq(status) : null;
    }

    private BooleanExpression productNameContains(String productName) {
       return productName != null ? sellingBid.productName.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression brandNameContains(String brandName) {
       return brandName != null ? sellingBid.brandName.containsIgnoreCase(brandName) : null;
    }

    private BooleanExpression periodBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null)
            return null;

        if (startDate != null && endDate != null) {
            return sellingBid.createdAt.between(
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay());
        }
        if (startDate != null) {
            return sellingBid.createdAt.goe(startDate.atStartOfDay());
        }
        return sellingBid.createdAt.lt(endDate.plusDays(1).atStartOfDay());
    }
}