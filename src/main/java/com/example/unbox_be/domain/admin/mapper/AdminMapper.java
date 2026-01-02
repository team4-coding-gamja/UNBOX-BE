package com.example.unbox_be.domain.admin.mapper;

import com.example.unbox_be.domain.admin.dto.response.*;
import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;

public class AdminMapper {

    public static AdminMeResponseDto toAdminMeResponseDto(Admin admin) {
        return AdminMeResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .adminStatus(admin.getAdminStatus())
                .build();
    }

    public static AdminBrandCreateResponseDto toAdminBrandCreateResponseDto(Brand brand) {
        return AdminBrandCreateResponseDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .logoUrl(brand.getLogoUrl())
                .build();
    }

    public static AdminProductCreateResponseDto toAdminProductCreateResponseDto(Product product) {
        return AdminProductCreateResponseDto.builder()
                .brandId(product.getBrand().getId())
                .id(product.getId())
                .name(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .build();
    }

    public static AdminProductOptionCreateResponseDto toAdminProductOptionCreateResponseDto(ProductOption productOption) {
        return AdminProductOptionCreateResponseDto.builder()
                .id(productOption.getId())
                .productId(productOption.getProduct().getId())
                .option(productOption.getOption())
                .build();
    }

    public static AdminMeUpdateResponseDto toAdminMeUpdateResponseDto(Admin admin){
        return AdminMeUpdateResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .build();
    }

    /**
     * 관리자(스태프) 목록 조회 DTO 변환
     */
    public static AdminStaffListResponseDto toAdminStaffPageResponseDto(Admin admin) {
        return AdminStaffListResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .build();
    }

    /**
     * 관리자(스태프) 상세 조회 DTO 변환
     */
    public static AdminStaffDetailResponseDto toAdminStaffDetailResponseDto(Admin admin) {
        return AdminStaffDetailResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .build();
    }

    /**
     * 관리자(스태프) 정보 수정 응답 DTO 변환
     */
    public static AdminStaffUpdateResponseDto toAdminStaffUpdateResponseDto(Admin admin) {
        return AdminStaffUpdateResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .build();
    }
}
