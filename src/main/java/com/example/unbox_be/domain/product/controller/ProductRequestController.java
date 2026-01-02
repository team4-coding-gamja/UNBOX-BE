package com.example.unbox_be.domain.product.controller;

import com.example.unbox_be.domain.product.dto.ProductRequestDto;
import com.example.unbox_be.domain.product.service.ProductRequestService;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;

@RestController
@RequestMapping("/api/product-requests")
@RequiredArgsConstructor
public class ProductRequestController {

    private final ProductRequestService productRequestService;

    @PostMapping
    public ApiResponse<String> createProductRequest(
            @RequestBody @Valid ProductRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        // 1. Controller는 이메일만 추출해서 Service로 전달
        String email = userDetails.getUsername();

        productRequestService.createProductRequest(email, requestDto);

        return ApiResponse.success("요청이 접수되었습니다.");
    }
}