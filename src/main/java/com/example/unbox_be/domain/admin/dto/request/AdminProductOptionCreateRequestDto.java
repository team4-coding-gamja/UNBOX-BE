package com.example.unbox_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminProductOptionCreateRequestDto {

    @NotBlank(message = "옵션 값은 필수입니다.")
    private String option;
}
