//package com.example.unbox_be.domain.reviews.repository;
//
//import com.example.unbox_be.domain.order.entity.Order;
//import com.example.unbox_be.domain.product.entity.Brand;
//import com.example.unbox_be.domain.product.entity.Category;
//import com.example.unbox_be.domain.product.entity.Product;
//import com.example.unbox_be.domain.product.entity.ProductOption;
//import com.example.unbox_be.domain.reviews.entity.Review;
//import com.example.unbox_be.domain.user.entity.User;
//import com.example.unbox_be.global.config.JpaAuditingConfig;
//import com.example.unbox_be.global.config.TestQueryDslConfig;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.data.domain.AuditorAware;
//import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//
//import java.math.BigDecimal;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
//@ActiveProfiles("test")
//@TestPropertySource(properties = {"spring.sql.init.mode=never"})
//class ReviewRepositoryTest {
//
//    @Autowired
//    private ReviewRepository reviewRepository; // ✅ 필드 주입으로 변경
//
//    @PersistenceContext
//    private EntityManager em;
//
//    // =========================================================
//    // ✅ 테스트 헬퍼 (연관 엔티티 포함: Brand -> Product -> Option -> Order -> Review)
//    // =========================================================
//
//    private User 유저생성(int n) {
//        return User.createUser(
//                "user" + n + "@test.com",
//                "pw" + n,
//                "user" + n,   // nickname: 소문자+숫자 4~10자 정규식 통과
//                "010-0000-00" + String.format("%02d", n)
//        );
//    }
//
//    private Brand 브랜드생성(int n) {
//        return Brand.createBrand("brand" + n, "https://example.com/logo" + n + ".png");
//    }
//
//    private Product 상품생성(Brand brand, int n) {
//        Category category = Category.SHOES;
//        return Product.createProduct(
//                "product" + n,
//                "model-" + n,
//                category,
//                "https://example.com/p" + n + ".png",
//                brand
//        );
//    }
//
//    private ProductOption 옵션생성(Product product, int n) {
//        return ProductOption.createProductOption(product, "옵션-" + n);
//    }
//
//    private Order 주문생성(User buyer, User seller, ProductOption option, int n) {
//        return Order.builder()
//                .sellingBidId(UUID.randomUUID())
//                .buyer(buyer)
//                .seller(seller)
//                .productOption(option)
//                .price(BigDecimal.valueOf(100000 + n))
//                .receiverName("받는사람" + n)
//                .receiverPhone("010-1111-22" + String.format("%02d", n))
//                .receiverAddress("서울시 어딘가 " + n)
//                .receiverZipCode("12345")
//                .build();
//    }
//
//    private Review 리뷰생성(Order order, int n) {
//        return Review.createReview(order, "리뷰내용-" + n, 5, "https://example.com/r" + n + ".png");
//    }
//
//    private void flushAndClear() {
//        em.flush();
//        em.clear();
//    }
//
//    // =========================================================
//    // ✅ 테스트
//    // =========================================================
//
//    @Nested
//    @DisplayName("findByIdAndDeletedAtIsNull")
//    class FindByIdAndDeletedAtIsNull {
//
//        @Test
//        @DisplayName("삭제되지 않은 리뷰는 조회된다")
//        void find_success_when_not_deleted() {
//            // given
//            User buyer = 유저생성(1);
//            User seller = 유저생성(2);
//            em.persist(buyer);
//            em.persist(seller);
//
//            Brand brand = 브랜드생성(1);
//            em.persist(brand);
//
//            Product product = 상품생성(brand, 1);
//            em.persist(product);
//
//            ProductOption option = 옵션생성(product, 1);
//            em.persist(option);
//
//            Order order = 주문생성(buyer, seller, option, 1);
//            em.persist(order);
//
//            Review review = 리뷰생성(order, 1);
//            em.persist(review);
//
//            flushAndClear();
//
//            // when
//            Optional<Review> found = reviewRepository.findByIdAndDeletedAtIsNull(review.getId());
//
//            // then
//            assertThat(found).isPresent();
//            assertThat(found.get().getContent()).isEqualTo("리뷰내용-1");
//            assertThat(found.get().getRating()).isEqualTo(5);
//            assertThat(found.get().getOrder().getId()).isEqualTo(order.getId());
//        }
//
//        @Test
//        @DisplayName("softDelete 된 리뷰는 조회되지 않는다")
//        void find_empty_when_soft_deleted() {
//            // given
//            User buyer = 유저생성(1);
//            User seller = 유저생성(2);
//            em.persist(buyer);
//            em.persist(seller);
//
//            Brand brand = 브랜드생성(1);
//            em.persist(brand);
//
//            Product product = 상품생성(brand, 1);
//            em.persist(product);
//
//            ProductOption option = 옵션생성(product, 1);
//            em.persist(option);
//
//            Order order = 주문생성(buyer, seller, option, 1);
//            em.persist(order);
//
//            Review review = 리뷰생성(order, 1);
//            em.persist(review);
//            flushAndClear();
//
//            // when (soft delete)
//            Review managed = em.find(Review.class, review.getId());
//            managed.softDelete("tester");
//            flushAndClear();
//
//            Optional<Review> found = reviewRepository.findByIdAndDeletedAtIsNull(review.getId());
//
//            // then
//            assertThat(found).isEmpty();
//        }
//    }
//
//    @Nested
//    @DisplayName("existsByOrderIdAndDeletedAtIsNull")
//    class ExistsByOrderIdAndDeletedAtIsNull {
//
//        @Test
//        @DisplayName("주문에 리뷰가 존재하면 true")
//        void exists_true_when_review_exists() {
//            // given
//            User buyer = 유저생성(1);
//            User seller = 유저생성(2);
//            em.persist(buyer);
//            em.persist(seller);
//
//            Brand brand = 브랜드생성(1);
//            em.persist(brand);
//
//            Product product = 상품생성(brand, 1);
//            em.persist(product);
//
//            ProductOption option = 옵션생성(product, 1);
//            em.persist(option);
//
//            Order order = 주문생성(buyer, seller, option, 1);
//            em.persist(order);
//
//            Review review = 리뷰생성(order, 1);
//            em.persist(review);
//            flushAndClear();
//
//            // when
//            boolean exists = reviewRepository.existsByOrderIdAndDeletedAtIsNull(order.getId());
//
//            // then
//            assertThat(exists).isTrue();
//        }
//
//        @Test
//        @DisplayName("리뷰가 softDelete 되면 false")
//        void exists_false_when_soft_deleted() {
//            // given
//            User buyer = 유저생성(1);
//            User seller = 유저생성(2);
//            em.persist(buyer);
//            em.persist(seller);
//
//            Brand brand = 브랜드생성(1);
//            em.persist(brand);
//
//            Product product = 상품생성(brand, 1);
//            em.persist(product);
//
//            ProductOption option = 옵션생성(product, 1);
//            em.persist(option);
//
//            Order order = 주문생성(buyer, seller, option, 1);
//            em.persist(order);
//
//            Review review = 리뷰생성(order, 1);
//            em.persist(review);
//            flushAndClear();
//
//            // when
//            Review managed = em.find(Review.class, review.getId());
//            managed.softDelete("tester");
//            flushAndClear();
//
//            boolean exists = reviewRepository.existsByOrderIdAndDeletedAtIsNull(order.getId());
//
//            // then
//            assertThat(exists).isFalse();
//        }
//    }
//
//    @Test
//    @DisplayName("1주문 1리뷰 원칙( order_id unique ) 때문에 같은 주문으로 리뷰 2개 저장 시 예외")
//    void unique_constraint_order_id() {
//        // given
//        User buyer = 유저생성(1);
//        User seller = 유저생성(2);
//        em.persist(buyer);
//        em.persist(seller);
//
//        Brand brand = 브랜드생성(1);
//        em.persist(brand);
//
//        Product product = 상품생성(brand, 1);
//        em.persist(product);
//
//        ProductOption option = 옵션생성(product, 1);
//        em.persist(option);
//
//        Order order = 주문생성(buyer, seller, option, 1);
//        em.persist(order);
//
//        Review review1 = 리뷰생성(order, 1);
//        Review review2 = 리뷰생성(order, 2); // 같은 주문
//
//        em.persist(review1);
//        em.persist(review2);
//
//        // then
//        assertThatThrownBy(() -> em.flush())
//                .isInstanceOfAny(DataIntegrityViolationException.class, RuntimeException.class);
//    }
//}