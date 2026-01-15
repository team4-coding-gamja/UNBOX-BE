package com.example.unbox_be.user.request.controller;

import com.example.unbox_be.user.request.controller.api.ProductRequestApi;
import com.example.unbox_be.user.request.dto.request.ProductRequestRequestDto;
import com.example.unbox_be.user.request.dto.response.ProductRequestResponseDto;
import com.example.unbox_be.user.request.service.ProductRequestService;
import com.example.unbox_be.common.response.CustomApiResponse;
import com.example.unbox_be.common.security.auth.CustomUserDetails;
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
