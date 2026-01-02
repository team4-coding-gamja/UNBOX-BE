package com.example.unbox_be.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderTrackingRequestDto {

    @NotBlank(message = "운송장 번호를 입력해주세요.")
    private String trackingNumber;
}