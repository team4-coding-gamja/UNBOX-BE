package com.example.unbox_user.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressCreateRequestDto {

    @Schema(description = "수령인 이름", example = "홍길동")
    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String receiverName;

    @Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @Schema(description = "상세 주소", example = "101호")
    private String detailAddress;

    @Schema(description = "우편번호", example = "06234")
    @NotBlank(message = "우편번호는 필수입니다.")
    private String zipCode;

    @Schema(description = "기본 배송지 여부", example = "true")
    private boolean isDefault;
}
