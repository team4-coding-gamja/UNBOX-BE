package com.example.unbox_be.domain.ai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AiReviewSummaryResponseDto {
    private String summary;
    private UUID productId;
    private int reviewCount;

    public static AiReviewSummaryResponseDto of(String summary, UUID productId, int reviewCount) {
        return AiReviewSummaryResponseDto.builder()
                .summary(summary)
                .productId(productId)
                .reviewCount(reviewCount)
                .build();
    }
}
