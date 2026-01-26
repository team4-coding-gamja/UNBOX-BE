package com.example.unbox_user.user.service;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_user.user.dto.request.AddressCreateRequestDto;
import com.example.unbox_user.user.dto.response.AddressResponseDto;
import com.example.unbox_user.user.entity.Address;
import com.example.unbox_user.user.entity.User;
import com.example.unbox_user.user.repository.AddressRepository;
import com.example.unbox_user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressResponseDto registerAddress(Long userId, AddressCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 첫 배송지인 경우 자동으로 기본 배송지로 설정
        boolean isFirstAddress = addressRepository.findAllByUserIdAndDeletedAtIsNull(userId).isEmpty();
        boolean isDefault = request.isDefault() || isFirstAddress;

        if (isDefault) {
             addressRepository.findByUserIdAndIsDefaultTrue(userId)
                     .ifPresent(address -> address.updateDefault(false));
        }
        
        // Address 객체 생성 (Builder 사용)
        Address address = Address.builder()
                .receiver_name(request.getReceiverName())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .zipCode(request.getZipCode())
                .isDefault(isDefault)
                .user(user)
                .build();
        
        return AddressResponseDto.from(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponseDto> getMyAddresses(Long userId) {
        return addressRepository.findAllByUserIdAndDeletedAtIsNull(userId).stream()
                .map(AddressResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAddress(Long userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        address.softDelete("USER_REQUEST");
    }
}
