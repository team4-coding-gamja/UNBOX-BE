package com.example.unbox_be.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder // DTO는 모든 필드 입력 가능하므로 클래스 레벨 빌더 허용
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

    @NotNull(message = "판매자 ID는 필수입니다.")
    private Long sellerId;

    @NotNull(message = "상품 옵션 ID는 필수입니다.")
    private UUID productOptionId;

    @NotNull(message = "가격 정보는 필수입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private BigDecimal price;

    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String receiverName;

    @NotBlank(message = "수령인 전화번호는 필수입니다.")
    private String receiverPhone;

    @NotBlank(message = "배송지 주소는 필수입니다.")
    private String receiverAddress;

    @NotBlank(message = "우편번호는 필수입니다.")
    private String receiverZipCode;
}