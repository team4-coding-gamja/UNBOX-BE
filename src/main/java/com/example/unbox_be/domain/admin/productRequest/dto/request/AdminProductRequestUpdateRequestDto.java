package com.example.unbox_be.domain.admin.productRequest.dto.request;

import com.example.unbox_be.domain.product.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductRequestUpdateRequestDto {
    private RequestStatus status;
}
