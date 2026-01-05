package com.example.unbox_be.domain.admin.productRequest.dto.request;

import com.example.unbox_be.domain.product.entity.ProductRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductRequestUpdateRequestDto {
    private ProductRequestStatus status;
}
