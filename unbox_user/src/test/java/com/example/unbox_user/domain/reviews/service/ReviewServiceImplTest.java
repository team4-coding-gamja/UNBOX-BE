//package com.example.unbox_be.domain.reviews.service;
//
//import com.example.unbox_be.domain.order.entity.Order;
//import com.example.unbox_be.domain.order.entity.OrderStatus;
//import com.example.unbox_be.domain.order.repository.OrderRepository;
//import com.example.unbox_be.domain.product.entity.Product;
//import com.example.unbox_be.domain.product.entity.ProductOption;
//import com.example.unbox_be.domain.product.service.ProductService;
//import com.example.unbox_be.domain.reviews.dto.request.ReviewCreateRequestDto;
//import com.example.unbox_be.domain.reviews.dto.request.ReviewUpdateRequestDto;
//import com.example.unbox_be.domain.reviews.dto.response.ReviewCreateResponseDto;
//import com.example.unbox_be.domain.reviews.dto.response.ReviewDetailResponseDto;
//import com.example.unbox_be.domain.reviews.dto.response.ReviewUpdateResponseDto;
//import com.example.unbox_be.domain.reviews.entity.Review;
//import com.example.unbox_be.domain.reviews.mapper.ReviewMapper;
//import com.example.unbox_be.domain.reviews.repository.ReviewRepository;
//import com.example.unbox_be.domain.user.entity.User;
//import com.example.unbox_be.global.error.exception.CustomException;
//import com.example.unbox_be.global.error.exception.ErrorCode;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ReviewServiceImplTest {
//
//    @InjectMocks
//    private ReviewServiceImpl reviewService;
//
//    @Mock private ReviewRepository reviewRepository;
//    @Mock private OrderRepository orderRepository;
//    @Mock private ReviewMapper reviewMapper;
//
//    @Mock private ProductService productService;
//
//    // =========================================================
//    // 공통 유틸 (✅ 전부 lenient 처리)
//    // =========================================================
//
//    private User 유저_아이디(Long id) {
//        User user = mock(User.class);
//        lenient().when(user.getId()).thenReturn(id);
//        return user;
//    }
//
//    private Product 상품() {
//        return mock(Product.class);
//    }
//
//    private ProductOption 상품옵션_상품(Product product) {
//        ProductOption option = mock(ProductOption.class);
//        lenient().when(option.getProduct()).thenReturn(product);
//        return option;
//    }
//
//    private Order 주문(OrderStatus status, User buyer, ProductOption option) {
//        Order order = mock(Order.class);
//        lenient().when(order.getStatus()).thenReturn(status);
//        lenient().when(order.getBuyer()).thenReturn(buyer);
//        lenient().when(order.getProductOption()).thenReturn(option);
//        return order;
//    }
//
//    private Review 리뷰_주문(Order order) {
//        Review review = mock(Review.class);
//        lenient().when(review.getOrder()).thenReturn(order);
//        return review;
//    }
//
//    private ReviewCreateRequestDto 리뷰생성요청(UUID orderId, String content, Integer rating, String imageUrl) {
//        ReviewCreateRequestDto dto = mock(ReviewCreateRequestDto.class);
//        lenient().when(dto.getOrderId()).thenReturn(orderId);
//        lenient().when(dto.getContent()).thenReturn(content);
//        lenient().when(dto.getRating()).thenReturn(rating);
//        lenient().when(dto.getImageUrl()).thenReturn(imageUrl);
//        return dto;
//    }
//
//    private ReviewUpdateRequestDto 리뷰수정요청(String content, Integer rating, String imageUrl) {
//        ReviewUpdateRequestDto dto = mock(ReviewUpdateRequestDto.class);
//        lenient().when(dto.getContent()).thenReturn(content);
//        lenient().when(dto.getRating()).thenReturn(rating);
//        lenient().when(dto.getImageUrl()).thenReturn(imageUrl);
//        return dto;
//    }
//
//    // =========================================================
//    // createReview
//    // =========================================================
//    @Nested
//    @DisplayName("리뷰 생성(createReview)")
//    class 리뷰생성 {
//
//        @Test
//        void 주문이_없으면_ORDER_NOT_FOUND_예외() {
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            ReviewCreateRequestDto req = 리뷰생성요청(orderId, "좋아요", 5, "img");
//
//            when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.empty());
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.createReview(userId, req), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
//
//            verify(reviewRepository, never()).save(any());
//            verify(reviewMapper, never()).toReviewCreateResponseDto(any());
//        }
//
//        @Test
//        void 주문상태가_COMPLETED가_아니면_ORDER_NOT_COMPLETED_예외() {
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            ReviewCreateRequestDto req = 리뷰생성요청(orderId, "좋아요", 5, "img");
//
//            User buyer = 유저_아이디(userId);
//            Product product = 상품();
//            ProductOption option = 상품옵션_상품(product);
//            Order order = 주문(OrderStatus.PENDING_SHIPMENT, buyer, option);
//
//            when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(order));
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.createReview(userId, req), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_COMPLETED);
//
//            verify(reviewRepository, never()).existsByOrderIdAndDeletedAtIsNull(any());
//            verify(reviewRepository, never()).save(any());
//        }
//
//        @Test
//        void 이미_해당주문에_리뷰가_있으면_ALREADY_REVIEWED_예외() {
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            ReviewCreateRequestDto req = 리뷰생성요청(orderId, "좋아요", 5, "img");
//
//            User buyer = 유저_아이디(userId);
//            Product product = 상품();
//            ProductOption option = 상품옵션_상품(product);
//            Order order = 주문(OrderStatus.COMPLETED, buyer, option);
//
//            when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(order));
//            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(true);
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.createReview(userId, req), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_REVIEWED);
//
//            verify(reviewRepository, never()).save(any());
//            verify(reviewMapper, never()).toReviewCreateResponseDto(any());
//        }
//
//        @Test
//        void 구매자가_null이면_ACCESS_DENIED_예외() {
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            ReviewCreateRequestDto req = 리뷰생성요청(orderId, "좋아요", 5, "img");
//
//            Product product = 상품();
//            ProductOption option = 상품옵션_상품(product);
//            Order order = 주문(OrderStatus.COMPLETED, null, option);
//
//            when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(order));
//            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.createReview(userId, req), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
//            verify(reviewRepository, never()).save(any());
//        }
//
//        @Test
//        void 로그인유저가_구매자가_아니면_ACCESS_DENIED_예외() {
//            Long loginUserId = 1L;
//            Long buyerId = 999L;
//
//            UUID orderId = UUID.randomUUID();
//            ReviewCreateRequestDto req = 리뷰생성요청(orderId, "좋아요", 5, "img");
//
//            User buyer = 유저_아이디(buyerId);
//            Product product = 상품();
//            ProductOption option = 상품옵션_상품(product);
//            Order order = 주문(OrderStatus.COMPLETED, buyer, option);
//
//            when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(order));
//            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.createReview(loginUserId, req), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
//            verify(reviewRepository, never()).save(any());
//        }
//
//        @Test
//        void 주문에서_상품이_null이면_PRODUCT_NOT_FOUND_예외() {
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            ReviewCreateRequestDto req = 리뷰생성요청(orderId, "좋아요", 5, "img");
//
//            User buyer = 유저_아이디(userId);
//            ProductOption option = 상품옵션_상품(null);
//            Order order = 주문(OrderStatus.COMPLETED, buyer, option);
//
//            when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(order));
//            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.createReview(userId, req), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
//            verify(reviewRepository, never()).save(any());
//        }
//
//        @Test
//        void 리뷰생성_성공하면_save_호출되고_응답DTO로_매핑된다() {
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            ReviewCreateRequestDto req = 리뷰생성요청(orderId, "좋아요", 5, "img");
//
//            User buyer = 유저_아이디(userId);
//            Product product = 상품();
//            ProductOption option = 상품옵션_상품(product);
//            Order order = 주문(OrderStatus.COMPLETED, buyer, option);
//
//            Review created = mock(Review.class);
//            Review saved = mock(Review.class);
//            ReviewCreateResponseDto expected = mock(ReviewCreateResponseDto.class);
//
//            when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(order));
//            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
//
//            try (MockedStatic<Review> reviewStatic = Mockito.mockStatic(Review.class)) {
//                reviewStatic.when(() -> Review.createReview(order, "좋아요", 5, "img"))
//                        .thenReturn(created);
//
//                when(reviewRepository.save(created)).thenReturn(saved);
//                when(reviewMapper.toReviewCreateResponseDto(saved)).thenReturn(expected);
//
//                ReviewCreateResponseDto result = reviewService.createReview(userId, req);
//
//                assertThat(result).isSameAs(expected);
//                verify(reviewRepository).save(created);
//                verify(reviewMapper).toReviewCreateResponseDto(saved);
//            }
//        }
//
//        @Test
//        void 리뷰생성_성공시_중복체크가_반드시_먼저_호출된다() {
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            ReviewCreateRequestDto req = 리뷰생성요청(orderId, "좋아요", 5, "img");
//
//            User buyer = 유저_아이디(userId);
//            Product product = 상품();
//            ProductOption option = 상품옵션_상품(product);
//            Order order = 주문(OrderStatus.COMPLETED, buyer, option);
//
//            when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(order));
//            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
//
//            Review created = mock(Review.class);
//            Review saved = mock(Review.class);
//            ReviewCreateResponseDto expected = mock(ReviewCreateResponseDto.class);
//
//            try (MockedStatic<Review> reviewStatic = Mockito.mockStatic(Review.class)) {
//                reviewStatic.when(() -> Review.createReview(any(), any(), any(), any()))
//                        .thenReturn(created);
//
//                when(reviewRepository.save(created)).thenReturn(saved);
//                when(reviewMapper.toReviewCreateResponseDto(saved)).thenReturn(expected);
//
//                reviewService.createReview(userId, req);
//
//                InOrder inOrder = inOrder(orderRepository, reviewRepository);
//                inOrder.verify(orderRepository).findByIdAndDeletedAtIsNull(orderId);
//                inOrder.verify(reviewRepository).existsByOrderIdAndDeletedAtIsNull(orderId);
//            }
//        }
//    }
//
//    // =========================================================
//    // getReview
//    // =========================================================
//    @Nested
//    @DisplayName("리뷰 조회(getReview)")
//    class 리뷰조회 {
//
//        @Test
//        void 리뷰가_없으면_REVIEW_NOT_FOUND_예외() {
//            UUID reviewId = UUID.randomUUID();
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.getReview(reviewId), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
//            verify(reviewMapper, never()).toReviewDetailResponseDto(any());
//        }
//
//        @Test
//        void 리뷰조회_성공하면_상세DTO로_매핑된다() {
//            UUID reviewId = UUID.randomUUID();
//            Review review = mock(Review.class);
//            ReviewDetailResponseDto expected = mock(ReviewDetailResponseDto.class);
//
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
//            when(reviewMapper.toReviewDetailResponseDto(review)).thenReturn(expected);
//
//            ReviewDetailResponseDto result = reviewService.getReview(reviewId);
//
//            assertThat(result).isSameAs(expected);
//            verify(reviewMapper).toReviewDetailResponseDto(review);
//        }
//    }
//
//    // =========================================================
//    // updateReview
//    // =========================================================
//    @Nested
//    @DisplayName("리뷰 수정(updateReview)")
//    class 리뷰수정 {
//
//        @Test
//        void 리뷰가_없으면_REVIEW_NOT_FOUND_예외() {
//            Long userId = 1L;
//            UUID reviewId = UUID.randomUUID();
//            ReviewUpdateRequestDto req = 리뷰수정요청("수정", 4, "img2");
//
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.updateReview(userId, reviewId, req), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
//            verify(reviewMapper, never()).toReviewUpdateResponseDto(any());
//        }
//
//        @Test
//        void 리뷰작성자가_아니면_NOT_REVIEW_OWNER_예외() {
//            Long loginUserId = 1L;
//            Long ownerId = 999L;
//
//            UUID reviewId = UUID.randomUUID();
//            ReviewUpdateRequestDto req = 리뷰수정요청("수정", 4, "img2");
//
//            User owner = 유저_아이디(ownerId);
//            Order order = mock(Order.class);
//            when(order.getBuyer()).thenReturn(owner);
//
//            Review review = 리뷰_주문(order);
//
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.updateReview(loginUserId, reviewId, req), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_REVIEW_OWNER);
//
//            verify(review, never()).update(any(), any(), any());
//            verify(reviewMapper, never()).toReviewUpdateResponseDto(any());
//        }
//
//        @Test
//        void 리뷰수정_성공하면_update가_호출되고_응답DTO로_매핑된다() {
//            Long userId = 1L;
//            UUID reviewId = UUID.randomUUID();
//            ReviewUpdateRequestDto req = 리뷰수정요청("수정", 4, "img2");
//
//            User buyer = 유저_아이디(userId);
//            Order order = mock(Order.class);
//            when(order.getBuyer()).thenReturn(buyer);
//
//            Review review = 리뷰_주문(order);
//            ReviewUpdateResponseDto expected = mock(ReviewUpdateResponseDto.class);
//
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
//            when(reviewMapper.toReviewUpdateResponseDto(review)).thenReturn(expected);
//
//            ReviewUpdateResponseDto result = reviewService.updateReview(userId, reviewId, req);
//
//            assertThat(result).isSameAs(expected);
//            verify(review).update("수정", 4, "img2");
//            verify(reviewMapper).toReviewUpdateResponseDto(review);
//        }
//
//        @Test
//        void 리뷰수정_성공시_save를_호출하지_않는다_더티체킹_전제() {
//            Long userId = 1L;
//            UUID reviewId = UUID.randomUUID();
//            ReviewUpdateRequestDto req = 리뷰수정요청("수정", 4, "img2");
//
//            User buyer = 유저_아이디(userId);
//            Order order = mock(Order.class);
//            when(order.getBuyer()).thenReturn(buyer);
//
//            Review review = 리뷰_주문(order);
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
//            when(reviewMapper.toReviewUpdateResponseDto(review)).thenReturn(mock(ReviewUpdateResponseDto.class));
//
//            reviewService.updateReview(userId, reviewId, req);
//
//            verify(reviewRepository, never()).save(any());
//        }
//    }
//
//    // =========================================================
//    // deleteReview
//    // =========================================================
//    @Nested
//    @DisplayName("리뷰 삭제(deleteReview)")
//    class 리뷰삭제 {
//
//        @Test
//        void 리뷰가_없으면_REVIEW_NOT_FOUND_예외() {
//            Long userId = 1L;
//            UUID reviewId = UUID.randomUUID();
//
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.deleteReview(userId, reviewId, "admin"), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
//        }
//
//        @Test
//        void 리뷰작성자가_아니면_NOT_REVIEW_OWNER_예외() {
//            Long loginUserId = 1L;
//            Long ownerId = 999L;
//            UUID reviewId = UUID.randomUUID();
//
//            User owner = 유저_아이디(ownerId);
//            Order order = mock(Order.class);
//            when(order.getBuyer()).thenReturn(owner);
//
//            Review review = 리뷰_주문(order);
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
//
//            CustomException ex = catchThrowableOfType(() -> reviewService.deleteReview(loginUserId, reviewId, "admin"), CustomException.class);
//
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_REVIEW_OWNER);
//            verify(review, never()).softDelete(any());
//        }
//
//        @Test
//        void 리뷰삭제_성공하면_softDelete가_호출된다() {
//            Long userId = 1L;
//            UUID reviewId = UUID.randomUUID();
//            String deletedBy = "admin";
//
//            User buyer = 유저_아이디(userId);
//            Order order = mock(Order.class);
//            when(order.getBuyer()).thenReturn(buyer);
//
//            Review review = 리뷰_주문(order);
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
//
//            reviewService.deleteReview(userId, reviewId, deletedBy);
//
//            verify(review).softDelete(deletedBy);
//        }
//
//        @Test
//        void 리뷰삭제는_delete나_save를_호출하지_않는다_소프트삭제_전제() {
//            Long userId = 1L;
//            UUID reviewId = UUID.randomUUID();
//
//            User buyer = 유저_아이디(userId);
//            Order order = mock(Order.class);
//            when(order.getBuyer()).thenReturn(buyer);
//
//            Review review = 리뷰_주문(order);
//            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
//
//            reviewService.deleteReview(userId, reviewId, "admin");
//
//            verify(reviewRepository, never()).delete(any());
//            verify(reviewRepository, never()).save(any());
//        }
//    }
//}
