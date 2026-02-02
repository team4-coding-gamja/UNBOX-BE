package com.example.unbox_user.request.presentation.dto.response;

import com.example.unbox_user.request.domain.entity.ProductRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestResponseDto {

    private UUID id;
    private String name;
    private String brandName;
    private ProductRequestStatus status;
}
