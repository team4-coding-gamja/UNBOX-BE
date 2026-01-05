package com.example.unbox_be.domain.admin.productRequest.service;

import com.example.unbox_be.domain.admin.productRequest.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.admin.productRequest.dto.response.AdminProductRequestUpdateResponseDto;
import com.example.unbox_be.domain.admin.productRequest.mapper.AdminProductRequestMapper;
import com.example.unbox_be.domain.product.entity.ProductRequest;
import com.example.unbox_be.domain.product.repository.ProductRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductRequestServiceImpl implements AdminProductRequestService {

    private final ProductRequestRepository productRequestRepository;
    private final AdminProductRequestMapper adminProductRequestMapper;

    @Override
    public Page<AdminProductRequestListResponseDto> getProductRequests(Pageable pageable) {
        return productRequestRepository.findAll(pageable)
                .map(adminProductRequestMapper::toListResponseDto);
    }

    @Override
    @Transactional
    public AdminProductRequestUpdateResponseDto updateProductRequestStatus(UUID id, AdminProductRequestUpdateRequestDto requestDto) {
        ProductRequest productRequest = productRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product request not found"));

        productRequest.updateStatus(requestDto.getStatus());

        return adminProductRequestMapper.toUpdateResponseDto(productRequest);
    }
}
