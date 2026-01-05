package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.request.ProductRequestRequestDto;
import com.example.unbox_be.domain.product.dto.response.ProductRequestResponseDto;
import com.example.unbox_be.domain.product.entity.ProductRequest;
import com.example.unbox_be.domain.product.entity.ProductRequestStatus;
import com.example.unbox_be.domain.product.mapper.ProductRequestMapper;
import com.example.unbox_be.domain.product.repository.ProductRequestRepository;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ProductRequestServiceImpl implements ProductRequestService {

    private final UserRepository userRepository;
    private final ProductRequestMapper productRequestMapper;
    private final ProductRequestRepository productRequestRepository;

    // ✅ 상품 등록 요청 생성
    @Override
    @Transactional
    public ProductRequestResponseDto createProductRequest(Long userId, ProductRequestRequestDto requestDto) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ProductRequest requestProduct = ProductRequest.createProductRequest(userId, requestDto.getName(), requestDto.getBrandName());
        ProductRequest savedProductRequest = productRequestRepository.save(requestProduct);
        return productRequestMapper.toProductRequestResponseDto(savedProductRequest);
    }
}
