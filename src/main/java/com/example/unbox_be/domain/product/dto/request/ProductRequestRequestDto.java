package com.example.unbox_be.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestRequestDto {

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(min = 1, max = 100, message = "상품명은 1자 이상 100자 이하로 입력해주세요.")
    private String name;

    @NotBlank(message = "브랜드명은 필수입니다.")
    @Size(min = 1, max = 50, message = "브랜드명은 1자 이상 50자 이하로 입력해주세요.")
    private String brandName;
}
