package com.example.unbox_be.domain.admin.productRequest.controller.api;

import com.example.unbox_be.domain.admin.productRequest.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestUpdateResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "관리자 상품 등록 요청", description = "관리자 상품 요청 관리 API")
@RequestMapping("/api/admin/product-requests")
public interface AdminProductRequestApi {

    @Operation(summary = "상품 요청 목록 조회", description = "상품 요청 목록을 페이징하여 조회합니다.")
    @GetMapping
    ResponseEntity<Page<AdminProductRequestListResponseDto>> getProductRequests(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0", required = true)
            @RequestParam int page,
            @Parameter(description = "페이지 크기", example = "10", required = true)
            @RequestParam int size
    );

    @Operation(summary = "상품 요청 상태 변경", description = "상품 요청의 상태를 변경합니다.")
    @PatchMapping("/{id}/status")
    ResponseEntity<AdminProductRequestUpdateResponseDto> updateProductRequestStatus(
            @PathVariable UUID id,
            @RequestBody AdminProductRequestUpdateRequestDto requestDto);
}
