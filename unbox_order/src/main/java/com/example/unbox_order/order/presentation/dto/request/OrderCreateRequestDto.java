package com.example.unbox_order.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

    @NotNull(message = "판매 입찰 ID는 필수입니다.")
    private UUID sellingBidId;

    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String receiverName;

    @NotBlank(message = "수령인 전화번호는 필수입니다.")
    private String receiverPhone;

    @NotBlank(message = "배송지 주소는 필수입니다.")
    private String receiverAddress;

    @NotBlank(message = "우편번호는 필수입니다.")
    private String receiverZipCode;
}