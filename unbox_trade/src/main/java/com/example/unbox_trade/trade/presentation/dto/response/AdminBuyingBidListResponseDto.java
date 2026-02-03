package com.example.unbox_trade.trade.presentation.dto.response;

import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBuyingBidListResponseDto {
    private UUID buyingBidId;
    private BuyingStatus status;
    private BigDecimal price;
    private LocalDateTime deadline; // 만료일이 있다면
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // 수정일 (선점/낙찰 시점 등 확인용)

    private Long buyerId; // 구매자 ID

    private UUID productOptionId;
    private String productOptionName;

    private UUID productId;
    private String productName;

    private UUID brandId;
    private String brandName;

    // 누가 마지막으로 수정했는지 (Admin 기능용)
    private String modifiedBy;
}
