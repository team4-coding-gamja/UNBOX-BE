package com.example.unbox_be.common.client.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSellingBidStatusRequest {
    private String status;  // "SOLD", "RESERVED" 등
    private String updatedBy;
}
