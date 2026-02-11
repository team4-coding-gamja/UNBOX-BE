package com.example.unbox_trade.trade.application.runner;

import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmingRunner implements ApplicationRunner {

    private final SellingBidRepository sellingBidRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) throws Exception {
        log.info("========== [Cache Warming] Started ==========");

        // 1. LIVE 상태인 매물이 있는 모든 옵션 ID 조회
        List<UUID> optionIds = sellingBidRepository.findAllProductOptionIdsWithLiveBids();
        
        if (optionIds.isEmpty()) {
            log.info("[Cache Warming] No LIVE bids found.");
            return;
        }

        int totalCount = 0;

        // 2. 각 옵션별로 순회하며 Redis에 적재
        for (UUID optionId : optionIds) {
            String queueKey = "bids:option:" + optionId;

            // 2-1. 기존 캐시 삭제 (중복 방지 - 서버 재시작 시 꼬이지 않게)
            redisTemplate.delete(queueKey);

            // 2-2. 해당 옵션의 LIVE 입찰 조회 (가격 낮은 순)
            List<SellingBid> bids = sellingBidRepository.findAllByProductOptionIdAndStatusAndDeletedAtIsNullOrderByPriceAsc(
                    optionId, SellingStatus.LIVE);

            if (bids.isEmpty()) continue;

            // 2-3. Redis List에 밀어 넣기 (RPUSH)
            List<String> bidIds = bids.stream()
                    .map(bid -> bid.getId().toString())
                    .toList();

            redisTemplate.opsForList().rightPushAll(queueKey, bidIds.toArray());
            
            totalCount += bidIds.size();
            // log.info("Loaded {} bids for option {}", bidIds.size(), optionId); // 너무 많으면 주석 처리
        }

        log.info("========== [Cache Warming] Completed (Total {} bids loaded) ==========", totalCount);
    }
}