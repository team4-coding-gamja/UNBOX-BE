package com.example.unbox_be.domain.admin.service;

import com.example.unbox_be.domain.admin.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.dto.response.AdminProductOptionCreateResponseDto;

import java.util.UUID;

public interface AdminProductService {

    AdminProductCreateResponseDto createProduct(String email, AdminProductCreateRequestDto requestDto);

    void deleteProduct(String email, UUID productId);

    AdminProductOptionCreateResponseDto createProductOption(String email, UUID productId, AdminProductOptionCreateRequestDto requestDto);

    void deleteProductOption(String email, UUID productId, UUID optionId);
}

