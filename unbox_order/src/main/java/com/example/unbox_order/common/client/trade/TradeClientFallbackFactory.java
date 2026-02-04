package com.example.unbox_order.common.client.trade;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.error.exception.FeignClientException;
import com.example.unbox_order.common.client.trade.dto.BuyingBidForOrderResponse;
import com.example.unbox_order.common.client.trade.dto.SellingBidForOrderResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * TradeClient의 FallbackFactory 클래스.
 * 
 * 기존 Fallback 방식의 문제점:
 * - Feign 호출 중 발생한 비즈니스 예외(예: "이미 예약된 입찰")도 SERVICE_UNAVAILABLE로 처리됨
 * 
 * FallbackFactory 방식의 이점:
 * - 원래 예외(cause)를 확인하여 비즈니스 예외는 그대로 전파
 * - Circuit Breaker가 열려서 호출이 차단된 경우에만 SERVICE_UNAVAILABLE 반환
 */
@Slf4j
@Component
public class TradeClientFallbackFactory implements FallbackFactory<TradeClient> {

    @Override
    public TradeClient create(Throwable cause) {
        return new TradeClient() {
            
            @Override
            public SellingBidForOrderResponse getSellingBidForOrder(UUID sellingBidId) {
                handleFallback("getSellingBidForOrder", sellingBidId, cause);
                return null; // unreachable
            }

            @Override
            public void reserveSellingBid(UUID sellingBidId, String updatedBy) {
                handleFallback("reserveSellingBid", sellingBidId, cause);
            }

            @Override
            public void soldSellingBid(UUID sellingBidId, String updatedBy) {
                handleFallback("soldSellingBid", sellingBidId, cause);
            }

            @Override
            public void liveSellingBid(UUID sellingBidId, String updatedBy) {
                handleFallback("liveSellingBid", sellingBidId, cause);
            }

            @Override
            public BuyingBidForOrderResponse getBuyingBidForOrder(UUID buyingBidId) {
                handleFallback("getBuyingBidForOrder", buyingBidId, cause);
                return null; // unreachable
            }

            @Override
            public void reserveBuyingBid(UUID buyingBidId, String updatedBy) {
                handleFallback("reserveBuyingBid", buyingBidId, cause);
            }

            @Override
            public void soldBuyingBid(UUID buyingBidId, String updatedBy) {
                handleFallback("soldBuyingBid", buyingBidId, cause);
            }

            @Override
            public void liveBuyingBid(UUID buyingBidId, String updatedBy) {
                handleFallback("liveBuyingBid", buyingBidId, cause);
            }
        };
    }

    /**
     * Fallback 처리 로직
     * 
     * 1. FeignClientException (비즈니스 예외): 원래 예외 그대로 전파
     * 2. CallNotPermittedException (Circuit Breaker OPEN): SERVICE_UNAVAILABLE 반환
     * 3. 그 외 예외: SERVICE_UNAVAILABLE 반환
     */
    private void handleFallback(String methodName, Object param, Throwable cause) {
        // 1. FeignClientException: 비즈니스 예외 → 그대로 전파
        if (cause instanceof FeignClientException feignEx) {
            log.debug("[Feign Error] Trade 서비스 응답 에러 - {}({}): {}", 
                methodName, param, feignEx.getMessage());
            throw feignEx; // 원래 예외 전파
        }
        
        // 2. Circuit Breaker가 열려서 호출 차단됨
        if (cause instanceof CallNotPermittedException) {
            log.warn("[CircuitBreaker OPEN] Trade 서비스 호출 차단 - {}({})", methodName, param);
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        
        // 3. 기타 예외 (네트워크 오류, 타임아웃 등)
        String causeMsg = (cause != null) ? cause.getMessage() : "unknown cause";
        log.error("[Trade Client Error] {}({}) 호출 실패: {}", methodName, param, causeMsg);
        throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
    }
}
