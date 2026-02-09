package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_trade.trade.application.service.purchase.*;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.response.CustomApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "구매 테스트 (Trade)", description = "동시성 테스트용 입찰 구매 API")
@RestController
@RequestMapping("/api/trade/purchase/test")
@RequiredArgsConstructor
public class TestPurchaseController {

    private final PessimisticPurchaseService pessimisticPurchaseService;
    private final DistributedPurchaseService distributedPurchaseService;
    private final DistributedNextBestPurchaseService distributedNextBestPurchaseService;
    private final CachedDistributedPurchaseService cachedDistributedPurchaseService;

    @Operation(summary = "판매 입찰 구매 (동시성 테스트)", description = "특정 판매 입찰(SellingBid)을 구매하여 상태를 MATCHED로 변경합니다. stage 파라미터로 전략을 선택합니다.")
    @PostMapping
    public CustomApiResponse<Void> purchase(
            @RequestParam UUID sellingBidId,
            @RequestParam Long buyerId,
            @RequestParam(defaultValue = "stage1") String stage) {
        
        PurchaseService service = switch (stage) {
            case "stage1" -> pessimisticPurchaseService;
            case "stage2" -> distributedPurchaseService;
            case "stage3" -> distributedNextBestPurchaseService;
            case "stage4" -> cachedDistributedPurchaseService;
            default -> throw new IllegalArgumentException("Unknown stage: " + stage);
        };
        
        service.purchase(sellingBidId, buyerId);
        return CustomApiResponse.success(null);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(ex.getMessage());
    }
}
