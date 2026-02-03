package com.example.unbox_trade.trade.domain.repository;

import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidSearchCondition;
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

import static com.example.unbox_trade.trade.domain.entity.QBuyingBid.buyingBid;

@Repository
@RequiredArgsConstructor
public class AdminBuyingBidRepositoryCustomImpl implements AdminBuyingBidRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<BuyingBid> findAdminBuyingBids(BuyingBidSearchCondition condition, Pageable pageable) {

        List<BuyingBid> content = queryFactory
                .selectFrom(buyingBid)
                .where(
                        statusEq(condition.getStatus()),
                        productNameContains(condition.getProductName()),
                        brandNameContains(condition.getBrandName()),
                        periodBetween(condition.getStartDate(), condition.getEndDate()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(buyingBid.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(buyingBid.count())
                .from(buyingBid)
                .where(
                        statusEq(condition.getStatus()),
                        productNameContains(condition.getProductName()),
                        brandNameContains(condition.getBrandName()),
                        periodBetween(condition.getStartDate(), condition.getEndDate()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression statusEq(BuyingStatus status) {
        return status != null ? buyingBid.status.eq(status) : null;
    }

    private BooleanExpression productNameContains(String productName) {
        return productName != null ? buyingBid.productName.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression brandNameContains(String brandName) {
        return brandName != null ? buyingBid.brandName.containsIgnoreCase(brandName) : null;
    }

    private BooleanExpression periodBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null)
            return null;

        if (startDate != null && endDate != null) {
            return buyingBid.createdAt.between(
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay());
        }
        if (startDate != null) {
            return buyingBid.createdAt.goe(startDate.atStartOfDay());
        }
        return buyingBid.createdAt.lt(endDate.plusDays(1).atStartOfDay());
    }
}
