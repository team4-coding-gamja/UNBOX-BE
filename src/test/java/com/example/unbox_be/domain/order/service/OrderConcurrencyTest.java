package com.example.unbox_be.domain.order.service;

import com.example.unbox_be.domain.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SellingBidRepository sellingBidRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    private Long buyerId;
    private Long otherBuyerId;
    private UUID sellingBidId;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        // 1. 유저 생성 (createUser 정적 팩토리 메서드 사용)
        User seller = userRepository.save(User.createUser("seller@test.com", "pw", "seller123", "010-1234-5678"));
        User buyer1 = userRepository.save(User.createUser("buyer1@test.com", "pw", "buyer123", "010-1111-1111"));
        User buyer2 = userRepository.save(User.createUser("buyer2@test.com", "pw", "buyer234", "010-2222-2222"));

        this.buyerId = buyer1.getId();
        this.otherBuyerId = buyer2.getId();

        // 2. 상품 생성
        // Brand 생성 (이름, 로고URL 모두 필수)
        Brand brand = brandRepository.save(Brand.createBrand("TestBrand", "https://example.com/logo.png"));

        // Product 생성 (정적 팩토리 메서드)
        // createProduct(String name, String modelNumber, Category category, String imageUrl, Brand brand)
        Product product = productRepository.save(Product.createProduct(
                "TestProduct", 
                "M-123", 
                Category.SHOES, 
                "http://example.com/image.png", 
                brand
        ));

        // ProductOption 생성 (정적 팩토리 메서드)
        ProductOption option = productOptionRepository.save(ProductOption.createProductOption(product, "270"));

        // 3. 판매 입찰 생성 (재고 1개)
        SellingBid sellingBid = sellingBidRepository.save(SellingBid.builder()
                .userId(seller.getId())
                .productOption(option)
                .price(BigDecimal.valueOf(100000))
                .deadline(LocalDateTime.now().plusDays(7))
                .build());
        this.sellingBidId = sellingBid.getId();
    }

    @Test
    @DisplayName("동시에 두 명이 같은 매물을 구매하려 하면 1명만 성공해야 한다")
    void createOrder_concurrency() throws InterruptedException {
        // given
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        // 구매자 1 요청
        executorService.submit(() -> {
            try {
                // OrderCreateRequestDto 생성자 수정 (Builder 사용)
                OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                        .sellingBidId(sellingBidId)
                        .receiverName("B1")
                        .receiverPhone("010-1111-1111")
                        .receiverAddress("Seoul")
                        .receiverZipCode("12345")
                        .build();

                orderService.createOrder(request, buyerId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        // 구매자 2 요청
        executorService.submit(() -> {
            try {
                OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                        .sellingBidId(sellingBidId)
                        .receiverName("B2")
                        .receiverPhone("010-2222-2222")
                        .receiverAddress("Busan")
                        .receiverZipCode("54321")
                        .build();

                orderService.createOrder(request, otherBuyerId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        latch.await(); // 두 스레드가 끝날 때까지 대기

        // then
        assertThat(successCount.get()).isEqualTo(1); // 오직 1명만 성공
        assertThat(failCount.get()).isEqualTo(1);    // 1명은 실패
    }
}
