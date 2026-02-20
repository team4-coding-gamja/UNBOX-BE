package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_trade.trade.application.service.purchase.*;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import com.example.unbox_trade.trade.presentation.dto.response.PurchaseQueueStatusResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "구매 테스트 (Trade)", description = "동시성 테스트용 입찰 구매 API")
@RestController
@RequestMapping("/api/trade/purchase/test")
@RequiredArgsConstructor
public class TestPurchaseController {

    // --- 기존 서비스 필드 ---
    private final PessimisticPurchaseService pessimisticPurchaseService;
    private final DistributedPurchaseService distributedPurchaseService;
    private final DistributedNextBestPurchaseService distributedNextBestPurchaseService;
    private final CachedDistributedPurchaseService cachedDistributedPurchaseService;
    private final LuaDistributedPurchaseService luaDistributedPurchaseService;
    private final QueuedPurchaseService queuedPurchaseService;

    // --- [추가해야 할 필드] ---
    // 이 두 줄이 없어서 에러가 났던 것입니다.
    private final SellingBidRepository sellingBidRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    @Operation(summary = "판매 입찰 구매 (동시성 테스트)", description = "특정 판매 입찰(SellingBid)을 구매하여 상태를 MATCHED로 변경합니다. stage 파라미터로 전략을 선택합니다.")
    @PostMapping
    public CustomApiResponse<?> purchase(
            @RequestParam UUID sellingBidId,
            @RequestParam Long buyerId,
            @RequestParam(defaultValue = "stage1") String stage) {

        if ("stage6".equals(stage)) {
            PurchaseQueueStatusResponseDto queueStatus = queuedPurchaseService.enqueue(sellingBidId, buyerId);
            return CustomApiResponse.success(queueStatus);
        }
        
        PurchaseService service = switch (stage) {
            case "stage1" -> pessimisticPurchaseService;
            case "stage2" -> distributedPurchaseService;
            case "stage3" -> distributedNextBestPurchaseService;
            case "stage4" -> cachedDistributedPurchaseService;
            case "stage5" -> luaDistributedPurchaseService;
            default -> throw new IllegalArgumentException("Unknown stage: " + stage);
        };
        
        service.purchase(sellingBidId, buyerId);
        return CustomApiResponse.successWithNoData();
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(ex.getMessage());
    }

    // --- [캐시 워밍 API] ---
    @PostMapping("/warm-up/{optionId}")
    public ResponseEntity<String> warmUpCache(@PathVariable UUID optionId) {
        // 1. 해당 옵션의 'LIVE' 상태 입찰을 가격 낮은 순으로 모두 조회
        List<SellingBid> liveBids = sellingBidRepository.findAllByProductOptionIdAndStatusAndDeletedAtIsNullOrderByPriceAsc(
                optionId, SellingStatus.LIVE);

        if (liveBids.isEmpty()) {
            return ResponseEntity.ok("No LIVE bids found for option: " + optionId);
        }

        String queueKey = "bids:option:" + optionId;

        // 2. 기존 캐시 초기화 (중복 방지)
        redisTemplate.delete(queueKey);

        // 3. Redis List에 입찰 ID들을 한 번에 밀어넣기 (RPUSH)
        List<String> bidIds = liveBids.stream()
                .map(bid -> bid.getId().toString())
                .toList();

        redisTemplate.opsForList().rightPushAll(queueKey, bidIds.toArray());

        return ResponseEntity.ok("Warmed up " + bidIds.size() + " bids for option: " + optionId);
    }
}
