package com.example.unbox_be.domain.admin.dto.request;

import com.example.unbox_be.domain.product.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductCreateRequestDto {

    @NotNull(message = "브랜드 ID는 필수입니다.")
    private UUID brandId;

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotBlank(message = "모델 번호는 필수입니다.")
    private String modelNumber;

    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;

    @NotBlank(message = "상품 이미지 URL은 필수입니다.")
    private String imageUrl;
}
