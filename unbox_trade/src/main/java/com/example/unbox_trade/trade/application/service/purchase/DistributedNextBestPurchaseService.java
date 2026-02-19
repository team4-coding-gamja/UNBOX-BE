package com.example.unbox_trade.trade.application.service.purchase;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
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
public class DistributedNextBestPurchaseService implements PurchaseService {

    private final RedissonClient redissonClient;
    private final SellingBidRepository sellingBidRepository;
    private final PurchaseTransactionService purchaseTransactionService;

    @Override
    public void purchase(UUID sellingBidId, Long buyerId) {
        // 1. 초기 요청된 입찰 정보 조회 (Option ID를 알기 위해)
        SellingBid originalBid = sellingBidRepository.findById(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));

        UUID optionId = originalBid.getProductOptionId();

        // 2. 해당 상품 옵션 전체에 대해 락을 걸음 (순차 매칭 보장)
        RLock optionLock = redissonClient.getLock("purchase:option:" + optionId);
        
        try {
            // waitTime을 어느 정도 줘서(예: 3s) 순차적으로 처리되게 함
            boolean available = optionLock.tryLock(10, 15, TimeUnit.SECONDS);
            if (!available) {
                throw new IllegalStateException("접속자가 많아 구매 처리가 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
            }

            // 3. 락 획득 후, 현재 가장 저렴한 LIVE 입찰을 다시 조회 (Next Best)
            SellingBid bestBid = sellingBidRepository.findFirstByProductOptionIdAndStatusAndDeletedAtIsNullOrderByPriceAsc(
                    optionId, SellingStatus.LIVE)
                    .orElseThrow(() -> new CustomException("현재 구매 가능한 입찰이 없습니다.", ErrorCode.BID_ALREADY_MATCHED));

            // 4. 매칭 및 상태 변경 트랜잭션 수행
            purchaseTransactionService.decreaseStock(bestBid.getId(), buyerId);

            log.info("Successfully matched Next Best bid: optionId={}, bidId={}, buyerId={}", 
                    optionId, bestBid.getId(), buyerId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("구매 처리 중 오류가 발생했습니다.");
        } finally {
            if (optionLock.isHeldByCurrentThread()) {
                optionLock.unlock();
            }
        }
    }
}
