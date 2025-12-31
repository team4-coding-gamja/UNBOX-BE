package com.example.unbox_be.domain.product.controller;

import com.example.unbox_be.domain.product.dto.ProductRequestDto;
import com.example.unbox_be.domain.product.service.ProductRequestService;
import com.example.unbox_be.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/product-requests")
@RequiredArgsConstructor
public class ProductRequestController {

    private final ProductRequestService productRequestService;

    @PostMapping
    public ApiResponse<String> createProductRequest(
            @RequestBody ProductRequestDto requestDto,
            Principal principal // 로그인한 사용자 정보
    ) {
        // 1. Controller는 이메일만 추출해서 Service로 전달
        String email = principal.getName();

        productRequestService.createProductRequest(email, requestDto);

        return ApiResponse.success("요청이 접수되었습니다.");
    }
}