package com.example.unbox_be.domain.trade.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor // 이 어노테이션이 있어야 new SellingBidRequestDto(id, price)가 가능합니다.
@Builder
public class SellingBidRequestDto {
    @NotNull(message = "유저 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "option ID 없음.")
    private UUID optionId;

    @NotNull(message = "Price 값 없음.")
    @Positive(message = "가격은 0보다 커야함")
    private Integer price;
}
