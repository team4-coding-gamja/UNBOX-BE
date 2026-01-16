package com.example.unbox_be.product.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBrandUpdateRequestDto {

    @Schema(description = "브랜드명(부분 수정 가능, null이면 유지)", example = "Nike")
    @Size(min = 1, max = 50, message = "브랜드명은 1~50자여야 합니다.")
    private String brandName;

    @Schema(description = "로고 URL(부분 수정 가능, null이면 유지)", example = "https://cdn.example.com/nike.png")
    private String brandImageUrl;
}
