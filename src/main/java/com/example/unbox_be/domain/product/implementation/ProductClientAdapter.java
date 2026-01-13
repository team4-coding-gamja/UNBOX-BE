package com.example.unbox_be.domain.product.implementation;

//@Component
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class ProductClientAdapter implements ProductClient {
//
//    private final ProductOptionRepository productOptionRepository;
//
//    @Override
//    public ProductOptionForOrderInfoResponse getProductForOrder(UUID productOptionId) {
//        ProductOption productOption = productOptionRepository.findById(productOptionId)
//                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
//
//        Product product = productOption.getProduct();
//
//        return ProductOptionForOrderInfoResponse.builder()
//                .id(productOption.getId())
//                .productId(product.getId())
//                .productName(product.getName())
//                .modelNumber(product.getModelNumber())
//                .optionName(productOption.getOption())
//                .imageUrl(product.getImageUrl())
//                .brandId(product.getBrand().getId())
//                .brandName(product.getBrand().getName())
//                .build();
//    }
//}
