package com.example.unbox_be.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingRequestDto {

    @NotBlank(message = "운송장 번호를 입력해주세요.")
    private String trackingNumber;
}