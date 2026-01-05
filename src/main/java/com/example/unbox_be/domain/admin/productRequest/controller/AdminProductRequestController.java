package com.example.unbox_be.domain.admin.productRequest.controller;

import com.example.unbox_be.domain.admin.productRequest.controller.api.AdminProductRequestApi;
import com.example.unbox_be.domain.admin.productRequest.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestUpdateResponseDto;
import com.example.unbox_be.domain.admin.productRequest.service.AdminProductRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/product-requests")
public class AdminProductRequestController implements AdminProductRequestApi {

    private final AdminProductRequestService adminProductRequestService;

    @Override
    public ResponseEntity<Page<AdminProductRequestListResponseDto>> getProductRequests(int page, int size) {
        return ResponseEntity.ok(adminProductRequestService.getProductRequests(PageRequest.of(page, size)));
    }

    @Override
    public ResponseEntity<AdminProductRequestUpdateResponseDto> updateProductRequestStatus(UUID id, AdminProductRequestUpdateRequestDto requestDto) {
        return ResponseEntity.ok(adminProductRequestService.updateProductRequestStatus(id, requestDto));
    }
}
