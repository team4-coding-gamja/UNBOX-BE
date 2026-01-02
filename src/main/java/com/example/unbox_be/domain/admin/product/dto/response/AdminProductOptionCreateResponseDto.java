package com.example.unbox_be.domain.admin.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminProductOptionCreateResponseDto {

    private UUID optionId;
    private UUID productId;
    private UUID id;
    private String option;

}
