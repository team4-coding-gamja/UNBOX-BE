package com.example.unbox_user.user.dto.response;

import com.example.unbox_user.user.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDto {

    @Schema(description = "배송지 ID")
    private UUID id;

    @Schema(description = "수령인 이름")
    private String receiverName;

    @Schema(description = "주소")
    private String address;

    @Schema(description = "상세 주소")
    private String detailAddress;

    @Schema(description = "우편번호")
    private String zipCode;

    @Schema(description = "기본 배송지 여부")
    private boolean isDefault;

    public static AddressResponseDto from(Address address) {
        return AddressResponseDto.builder()
                .id(address.getId())
                .receiverName(address.getReceiver_name()) // Note: field name is snake_case in entity
                .address(address.getAddress())
                .detailAddress(address.getDetailAddress())
                .zipCode(address.getZipCode())
                .isDefault(address.isDefault())
                .build();
    }
}
