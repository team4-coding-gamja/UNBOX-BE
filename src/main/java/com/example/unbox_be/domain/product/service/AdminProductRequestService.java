package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductRequestUpdateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminProductRequestService {

    // ✅ 상품 요청 목록 조회
    Page<AdminProductRequestListResponseDto> getProductRequests(Pageable pageable);
    // ✅ 상품 요청 상태 변경
    AdminProductRequestUpdateResponseDto updateProductRequestStatus(UUID id, AdminProductRequestUpdateRequestDto requestDto);
}
