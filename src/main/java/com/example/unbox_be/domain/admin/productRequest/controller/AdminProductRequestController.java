package com.example.unbox_be.domain.admin.productRequest.controller;

import com.example.unbox_be.domain.admin.productRequest.controller.api.AdminProductRequestApi;
import com.example.unbox_be.domain.admin.productRequest.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestUpdateResponseDto;
import com.example.unbox_be.domain.admin.productRequest.service.AdminProductRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminProductRequestController implements AdminProductRequestApi {

    private final AdminProductRequestService adminProductRequestService;

    @Override
    public ResponseEntity<Page<AdminProductRequestListResponseDto>> getProductRequests(Pageable pageable) {
        return ResponseEntity.ok(adminProductRequestService.getProductRequests(pageable));
    }

    @Override
    public ResponseEntity<AdminProductRequestUpdateResponseDto> updateProductRequestStatus(UUID id, AdminProductRequestUpdateRequestDto requestDto) {
        return ResponseEntity.ok(adminProductRequestService.updateProductRequestStatus(id, requestDto));
    }
}
