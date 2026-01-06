package com.example.unbox_be.domain.product.controller;

import com.example.unbox_be.domain.product.controller.api.ProductRequestApi;
import com.example.unbox_be.domain.product.dto.request.ProductRequestRequestDto;
import com.example.unbox_be.domain.product.dto.response.ProductRequestResponseDto;
import com.example.unbox_be.domain.product.service.ProductRequestService;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRequestController implements ProductRequestApi {

    private final ProductRequestService productRequestService;

    // ✅ 상품 등록 요청 생성
    @PostMapping("requests")
    public CustomApiResponse<ProductRequestResponseDto> createProductRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ProductRequestRequestDto requestDto){
        ProductRequestResponseDto result = productRequestService.createProductRequest(userDetails.getUserId(), requestDto);
        return CustomApiResponse.success(result);
    }
}
