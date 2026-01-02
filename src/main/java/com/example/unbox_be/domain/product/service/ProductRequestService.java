package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.ProductRequestDto;
import com.example.unbox_be.domain.product.entity.ProductRequest;
import com.example.unbox_be.domain.product.mapper.ProductRequestMapper;
import com.example.unbox_be.domain.product.repository.ProductRequestRepository;
import com.example.unbox_be.domain.user.entity.User; // User 엔티티 import
import com.example.unbox_be.domain.user.repository.UserRepository; // UserRepository import
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductRequestService {

    private final ProductRequestRepository productRequestRepository;
    private final ProductRequestMapper productRequestMapper;

    // [추가] Product 도메인이지만, 필요에 의해 User 저장소를 직접 사용
    private final UserRepository userRepository;

    // 파라미터가 UUID userId -> String email로 변경됨
    public UUID createProductRequest(Long userId, ProductRequestDto dto) {

        //UserId
        ProductRequest request = productRequestMapper.toEntity(dto, userId);

        //저장
        ProductRequest savedRequest = productRequestRepository.save(request);

        return savedRequest.getId();
    }
}