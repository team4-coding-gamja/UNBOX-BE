package com.example.unbox_be.product.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminBrandCreateRequestDto {

    @NotBlank(message = "브랜드명은 필수입니다.")
    private String name;

    @NotBlank(message = "브랜드 로고 URL은 필수입니다.")
    @Pattern(regexp = "^(http|https)://.*$", message = "로고 URL은 http 또는 https 형식이어야 합니다.")
    private String logoUrl;
}
