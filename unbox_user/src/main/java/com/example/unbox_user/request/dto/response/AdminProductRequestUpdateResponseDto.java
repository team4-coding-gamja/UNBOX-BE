package com.example.unbox_user.request.dto.response;

import com.example.unbox_user.request.entity.ProductRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductRequestUpdateResponseDto {

    private UUID id;
    private ProductRequestStatus status;
}
