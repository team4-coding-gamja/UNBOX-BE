package com.example.unbox_be.user.request.service;

import com.example.unbox_be.user.request.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.user.request.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.user.request.dto.response.AdminProductRequestUpdateResponseDto;
import com.example.unbox_be.user.request.mapper.AdminProductRequestMapper;
import com.example.unbox_be.user.request.entity.ProductRequest;
import com.example.unbox_be.user.request.repository.ProductRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductRequestServiceImpl implements AdminProductRequestService {

    private final ProductRequestRepository productRequestRepository;
    private final AdminProductRequestMapper adminProductRequestMapper;

    // ✅ 상품 요청 목록 조회
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public Page<AdminProductRequestListResponseDto> getProductRequests(Pageable pageable) {
        Page<ProductRequest> productRequests = productRequestRepository.findAll(pageable);
        return productRequests.map(adminProductRequestMapper::toListResponseDto);
    }

    // ✅ 상품 요청 상태 변경
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminProductRequestUpdateResponseDto updateProductRequestStatus(UUID id, AdminProductRequestUpdateRequestDto requestDto) {
        ProductRequest productRequest = productRequestRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Product request not found"));

        productRequest.updateStatus(requestDto.getStatus());

        return adminProductRequestMapper.toUpdateResponseDto(productRequest);
    }
}
