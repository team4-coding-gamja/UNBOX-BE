package com.example.unbox_order.common.client.trade;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_order.common.client.trade.dto.SellingBidForOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * TradeClient의 Fallback 클래스.
 * Circuit Breaker가 OPEN 상태일 때 즉시 실패를 반환하여
 * 스레드 블로킹 없이 빠른 실패(Fail-Fast)를 제공합니다.
 */
@Slf4j
@Component
public class TradeClientFallback implements TradeClient {

    @Override
    public SellingBidForOrderResponse getSellingBidForOrder(UUID sellingBidId) {
        log.warn("[CircuitBreaker OPEN] Trade 서비스 호출 실패 - getSellingBidForOrder({})", sellingBidId);
        throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public void reserveSellingBid(UUID sellingBidId, String updatedBy) {
        log.warn("[CircuitBreaker OPEN] Trade 서비스 호출 실패 - reserveSellingBid({}, {})", sellingBidId, updatedBy);
        throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public void soldSellingBid(UUID sellingBidId, String updatedBy) {
        log.warn("[CircuitBreaker OPEN] Trade 서비스 호출 실패 - soldSellingBid({}, {})", sellingBidId, updatedBy);
        throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public void liveSellingBid(UUID sellingBidId, String updatedBy) {
        log.warn("[CircuitBreaker OPEN] Trade 서비스 호출 실패 - liveSellingBid({}, {})", sellingBidId, updatedBy);
        throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
    }
}

