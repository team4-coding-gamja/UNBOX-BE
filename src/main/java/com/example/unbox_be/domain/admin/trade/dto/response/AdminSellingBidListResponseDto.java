package com.example.unbox_be.domain.admin.trade.dto.response;

import com.example.unbox_be.domain.trade.entity.SellingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSellingBidListResponseDto {
    private UUID id;
    private String productName;
    private String brandName;
    private String size;
    private Integer price;
    private SellingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
}
