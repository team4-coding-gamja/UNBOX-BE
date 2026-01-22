package com.example.unbox_user.request.service;

import com.example.unbox_user.request.dto.request.ProductRequestRequestDto;
import com.example.unbox_user.request.dto.response.ProductRequestResponseDto;
import com.example.unbox_user.request.entity.ProductRequest;
import com.example.unbox_user.request.mapper.ProductRequestMapper;
import com.example.unbox_user.request.repository.ProductRequestRepository;
import com.example.unbox_user.user.repository.UserRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
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
        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ProductRequest requestProduct = ProductRequest.createProductRequest(userId, requestDto.getName(), requestDto.getBrandName());
        ProductRequest savedProductRequest = productRequestRepository.save(requestProduct);
        return productRequestMapper.toProductRequestResponseDto(savedProductRequest);
    }
}
