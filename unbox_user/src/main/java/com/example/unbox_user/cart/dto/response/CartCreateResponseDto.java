package com.example.unbox_user.cart.dto.response;

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
public class CartCreateResponseDto {

    private UUID cartId;
    private LocalDateTime createdAt;

    private UUID sellingBidId;
}
