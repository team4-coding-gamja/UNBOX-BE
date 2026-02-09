package com.example.unbox_trade.trade.application.service.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedPurchaseService implements PurchaseService {

    private final RedissonClient redissonClient;
    private final PurchaseTransactionService purchaseTransactionService;

    @Override
    public void purchase(UUID sellingBidId, Long buyerId) {
        RLock lock = redissonClient.getLock("purchase:sellingBid:" + sellingBidId);
        try {
            // waitTime = 0 (Fail-fast), leaseTime = 10s
            boolean available = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!available) {
                // 락 획득 실패 시 바로 종료 (Fail-fast)
                throw new IllegalStateException("현재 접속자가 많아 구매가 불가능합니다.");
            }

            // 락 획득 성공 -> 트랜잭션 수행
            purchaseTransactionService.decreaseStock(sellingBidId, buyerId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("구매 처리 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
