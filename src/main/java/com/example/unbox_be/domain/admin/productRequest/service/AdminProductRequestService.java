package com.example.unbox_be.domain.admin.productRequest.service;

import com.example.unbox_be.domain.admin.productRequest.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestUpdateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminProductRequestService {
    Page<AdminProductRequestListResponseDto> getProductRequests(Pageable pageable);
    AdminProductRequestUpdateResponseDto updateProductRequestStatus(UUID id, AdminProductRequestUpdateRequestDto requestDto);
}
