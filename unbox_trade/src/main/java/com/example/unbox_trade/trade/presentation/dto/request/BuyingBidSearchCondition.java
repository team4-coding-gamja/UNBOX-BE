package com.example.unbox_trade.trade.presentation.dto.request;

import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BuyingBidSearchCondition {
    @Schema(description = "구매 입찰 상태")
    private BuyingStatus status;

    @Schema(description = "상품명 검색 (부분 일치)")
    private String productName;

    @Schema(description = "브랜드명 검색 (부분 일치)")
    private String brandName;

    @Schema(description = "검색 시작일")
    private LocalDate startDate;

    @Schema(description = "검색 종료일")
    private LocalDate endDate;
}
