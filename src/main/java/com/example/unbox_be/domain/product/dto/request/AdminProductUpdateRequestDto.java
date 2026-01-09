package com.example.unbox_be.domain.product.dto.request;

import com.example.unbox_be.domain.product.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductUpdateRequestDto {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotBlank(message = "모델 번호는 필수입니다.")
    private String modelNumber;

    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;

    @NotBlank(message = "상품 이미지 URL은 필수입니다.")
    @Pattern(regexp = "^(http|https)://.*$", message = "이미지 URL은 http 또는 https 형식이어야 합니다.")
    private String imageUrl;
}
