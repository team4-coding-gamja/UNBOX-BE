package com.example.unbox_be.product.product.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBrandUpdateResponseDto {

    private UUID id;
    private String name;
    private String logoUrl;
}
