package com.example.unbox_be.domain.trade.repository;

import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.global.config.JpaAuditingConfig;
import com.example.unbox_be.global.config.TestQueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
class SellingBidRepositoryTest {

    @Autowired
    private SellingBidRepository sellingBidRepository;

    @Autowired
    private TestEntityManager entityManager; // 연관 객체 생성을 위한 헬퍼
    private Product product;
    private ProductOption option;
    // 🚩 1. 필드 추가
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private <T> T createInstance(Class<T> clazz) {
        try {
            java.lang.reflect.Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true); // protected 생성자를 강제로 오픈
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("테스트 객체 생성 실패: " + clazz.getName(), e);
        }
    }

    @BeforeEach
    void setUp() {
        // 1. Brand 생성
        Brand brand = createInstance(Brand.class);
        ReflectionTestUtils.setField(brand, "name", "Nike");
        entityManager.persist(brand);

        // 2. Product 생성
        product = createInstance(Product.class);
        ReflectionTestUtils.setField(product, "name", "Jordan 1");
        ReflectionTestUtils.setField(product, "brand", brand);
        ReflectionTestUtils.setField(product, "category", Category.SHOES);
        entityManager.persist(product);

        // 3. ProductOption 생성
        option = createInstance(ProductOption.class);
        ReflectionTestUtils.setField(option, "product", product);
        ReflectionTestUtils.setField(option, "option", "270");
        entityManager.persist(option);

        entityManager.flush();
    }

    @Test
    @DisplayName("ID로 상세 정보 조회 및 비관적 락 확인")
    void findByIdAndDeletedAtIsNullForUpdate_Success() {
        // given
        SellingBid bid = SellingBid.builder()
                .price(BigDecimal.valueOf(10000))
                .status(SellingStatus.LIVE)
                .userId(1L)
                .build();
        SellingBid savedBid = sellingBidRepository.save(bid);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<SellingBid> result = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(savedBid.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }
    @Test
    @DisplayName("여러 상품 ID 목록에 대한 상품별 최저가 조회")
    void findLowestPricesByProductIds_Success() {
        // given
        // 1. 입찰 데이터 생성 및 연관관계(option) 설정
        SellingBid bid1 = createInstance(SellingBid.class);
        ReflectionTestUtils.setField(bid1, "price", BigDecimal.valueOf(10000));
        ReflectionTestUtils.setField(bid1, "status", SellingStatus.LIVE);
        ReflectionTestUtils.setField(bid1, "productOption", option); // @BeforeEach에서 만든 option
        ReflectionTestUtils.setField(bid1, "userId", 1L);

        sellingBidRepository.save(bid1);

        // 2. ID가 채워지도록 강제 반영
        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화 (실제 쿼리 확인용)

        // 🚩 이제 product.getId()는 null이 아닙니다.
        UUID targetProductId = product.getId();

        // when
        List<Object[]> results = sellingBidRepository.findLowestPricesByProductIds(List.of(targetProductId));

        // then
        assertThat(results.size()).isGreaterThan(0);
        // isNotEmpty 대신 size 기반 검증

        Object[] row = results.get(0);
        UUID foundId = (UUID) row[0];
        BigDecimal minPrice = (BigDecimal) row[1]; // BigDecimal로 캐스팅

        assertThat(foundId).isEqualTo(targetProductId);
        assertThat(minPrice).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }
    @Test
    @DisplayName("특정 상품 ID로 현재 판매 중인 최저가 하나만 조회")
    void findLowestPriceByProductId_Success() {
        // given
        SellingBid bid = createInstance(SellingBid.class);
        // 🚩 여기도 "id" 수동 주입 코드를 삭제하세요.
        ReflectionTestUtils.setField(bid, "price", BigDecimal.valueOf(5000));
        ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
        ReflectionTestUtils.setField(bid, "productOption", option);
        ReflectionTestUtils.setField(bid, "userId", 1L);

        sellingBidRepository.save(bid);
        entityManager.flush();

        // when
        Integer lowestPrice = sellingBidRepository.findLowestPriceByProductId(product.getId(), SellingStatus.LIVE);

        // then
        assertThat(lowestPrice).isNotNull();
        assertThat(lowestPrice).isEqualTo(5000);
    }
    @Test
    @DisplayName("같은 상품 내 여러 옵션 중 전체 최저가 조회")
    void findLowestPriceByProductId_MultipleOptions() {
        // given: 270 사이즈 10000원, 280 사이즈 8000원 입찰 존재
        ProductOption option2 = createInstance(ProductOption.class);
        ReflectionTestUtils.setField(option2, "product", product);
        ReflectionTestUtils.setField(option2, "option", "280");
        entityManager.persist(option2);

        saveSellingBid(BigDecimal.valueOf(10000), option, SellingStatus.LIVE);
        saveSellingBid(BigDecimal.valueOf(8000), option2, SellingStatus.LIVE);
        entityManager.flush();

        // when
        Integer lowestPrice = sellingBidRepository.findLowestPriceByProductId(product.getId(), SellingStatus.LIVE);

        // then: 옵션과 상관없이 상품 전체에서 가장 낮은 8000원이 나와야 함
        assertThat(lowestPrice).isEqualTo(8000);
    }

    @Test
    @DisplayName("LIVE 상태인 입찰만 최저가 계산에 포함")
    void findLowestPriceByProductId_OnlyLiveStatus() {
        // given: LIVE 10000원, MATCHED(거래완료) 5000원 존재
        saveSellingBid(BigDecimal.valueOf(10000), option, SellingStatus.LIVE);
        saveSellingBid(BigDecimal.valueOf(5000), option, SellingStatus.MATCHED);
        entityManager.flush();

        // when
        Integer lowestPrice = sellingBidRepository.findLowestPriceByProductId(product.getId(), SellingStatus.LIVE);

        // then: MATCHED 상태인 5000원은 무시되고 10000원이 조회되어야 함
        assertThat(lowestPrice).isEqualTo(10000);
    }

    @Test
    @DisplayName("소프트 삭제(deleted_at)된 입찰은 조회 제외")
    void findByIdAndDeletedAtIsNull_SoftDeleteTest() {
        // given
        SellingBid bid = saveSellingBid(BigDecimal.valueOf(10000), option, SellingStatus.LIVE);
        ReflectionTestUtils.setField(bid, "deletedAt", java.time.LocalDateTime.now()); // 소프트 삭제
        entityManager.flush();

        // when
        Optional<SellingBid> result = sellingBidRepository.findByIdAndDeletedAtIsNull(bid.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("입찰이 없는 상품 조회 시 null 반환 확인")
    void findLowestPriceByProductId_EmptyBids() {
        // when
        Integer lowestPrice = sellingBidRepository.findLowestPriceByProductId(product.getId(), SellingStatus.LIVE);

        // then
        assertThat(lowestPrice).isNull();
    }

    @Test
    @DisplayName("특정 사용자 ID로 판매 입찰 내역 Slice 페이징 조회")
    void findByUserId_PagingTest() {
        // given: 유저 1L의 입찰 3개 생성
        Long userId = 1L;
        saveSellingBid(BigDecimal.valueOf(10000), option, SellingStatus.LIVE, userId);
        saveSellingBid(BigDecimal.valueOf(11000), option, SellingStatus.LIVE, userId);
        saveSellingBid(BigDecimal.valueOf(12000), option, SellingStatus.LIVE, userId);
        entityManager.flush();

        // when: 첫 번째 페이지, 사이즈 2개 조회
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, 2);
        org.springframework.data.domain.Slice<SellingBid> slice = sellingBidRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        // then
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.hasNext()).isTrue(); // 다음 페이지 존재 확인
    }

    @Test
    @DisplayName("여러 옵션 ID들에 대한 각각의 최저가 그룹화 조회")
    void findLowestPriceByOptionIds_GroupingTest() {
        // given
        ProductOption option2 = createInstance(ProductOption.class);
        ReflectionTestUtils.setField(option2, "product", product);
        ReflectionTestUtils.setField(option2, "option", "280");
        entityManager.persist(option2);

        saveSellingBid(BigDecimal.valueOf(10000), option, SellingStatus.LIVE);
        saveSellingBid(BigDecimal.valueOf(9000), option2, SellingStatus.LIVE);
        entityManager.flush();

        // when
        List<Object[]> results = sellingBidRepository.findLowestPriceByOptionIds(List.of(option.getId(), option2.getId()));

        // then: 결과 리스트에 각 옵션별 최저가가 담겨있어야 함
        assertThat(results).hasSize(2);
    }

    // 헬퍼 메서드: 반복되는 입찰 생성을 간소화
    private SellingBid saveSellingBid(BigDecimal price, ProductOption opt, SellingStatus status) {
        return saveSellingBid(price, opt, status, 1L);
    }

    private SellingBid saveSellingBid(BigDecimal price, ProductOption opt, SellingStatus status, Long userId) {
        SellingBid bid = createInstance(SellingBid.class);
        ReflectionTestUtils.setField(bid, "price", price);
        ReflectionTestUtils.setField(bid, "status", status);
        ReflectionTestUtils.setField(bid, "productOption", opt);
        ReflectionTestUtils.setField(bid, "userId", userId);
        return sellingBidRepository.save(bid);
    }

    @Test
    @DisplayName("먼저 조회한 트랜잭션이 종료될 때까지 두 번째 트랜잭션은 대기한다")
    void pessimisticLock_BlockingTest() throws InterruptedException {
// 1. 새 트랜잭션 템플릿 준비
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 🚩 2. [핵심] 모든 연관 데이터를 새 트랜잭션에서 한꺼번에 생성 및 커밋
        UUID bidId = tt.execute(status -> {
            // Brand 생성
            Brand b = createInstance(Brand.class);
            ReflectionTestUtils.setField(b, "name", "Nike " + UUID.randomUUID());
            entityManager.persist(b);

            // Product 생성
            Product p = createInstance(Product.class);
            ReflectionTestUtils.setField(p, "name", "Jordan");
            ReflectionTestUtils.setField(p, "brand", b);
            ReflectionTestUtils.setField(p, "category", Category.SHOES);
            entityManager.persist(p);

            // Option 생성
            ProductOption po = createInstance(ProductOption.class);
            ReflectionTestUtils.setField(po, "product", p);
            ReflectionTestUtils.setField(po, "option", "270"); // PO_FIELD_NAME은 실제 필드명(예: "option")
            entityManager.persist(po);

            // SellingBid 생성
            SellingBid bid = createInstance(SellingBid.class);
            ReflectionTestUtils.setField(bid, "price", BigDecimal.valueOf(10000));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            ReflectionTestUtils.setField(bid, "productOption", po);
            ReflectionTestUtils.setField(bid, "userId", 1L);

            return sellingBidRepository.save(bid).getId();
        });

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        // Thread A: 락 획득 후 1초간 점유
        Thread threadA = new Thread(() -> {
            tt.execute(status -> {
                sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(bidId).orElseThrow();
                latch.countDown();
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                return null;
            });
        });

        threadA.start();
        latch.await();

        // when
        long startTime = System.currentTimeMillis();
        tt.execute(status -> {
            sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(bidId);
            return null;
        });

        long duration = System.currentTimeMillis() - startTime;
        threadA.join();

        // then
        assertThat(duration).isGreaterThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("동시에 가격 수정을 시도해도 락 덕분에 최종 결과가 보장된다")
    void pessimisticLock_DataIntegrityTest() throws InterruptedException {
        // 1. 트랜잭션 템플릿 준비 (NPE 방지 및 트랜잭션 분리)
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 🚩 2. 모든 연관 데이터를 새 트랜잭션에서 한꺼번에 생성 및 커밋 (외래 키 에러 방지)
        UUID bidId = tt.execute(status -> {
            Brand b = createInstance(Brand.class);
            ReflectionTestUtils.setField(b, "name", "Nike " + UUID.randomUUID());
            entityManager.persist(b);

            Product p = createInstance(Product.class);
            ReflectionTestUtils.setField(p, "name", "Jordan");
            ReflectionTestUtils.setField(p, "brand", b);
            ReflectionTestUtils.setField(p, "category", Category.SHOES);
            entityManager.persist(p);

            ProductOption po = createInstance(ProductOption.class);
            ReflectionTestUtils.setField(po, "product", p);
            ReflectionTestUtils.setField(po, "option", "270");
            entityManager.persist(po);

            SellingBid bid = createInstance(SellingBid.class);
            ReflectionTestUtils.setField(bid, "price", BigDecimal.valueOf(10000));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            ReflectionTestUtils.setField(bid, "productOption", po);
            ReflectionTestUtils.setField(bid, "userId", 1L);

            return sellingBidRepository.save(bid).getId();
        });

        // 3. 동시 수정 시뮬레이션 설정
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(2);

        // 트랜잭션 A: +1000원
        executor.execute(() -> {
            try {
                tt.execute(status -> {
                    SellingBid b = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(bidId).orElseThrow();
                    ReflectionTestUtils.setField(b, "price", b.getPrice().add(BigDecimal.valueOf(1000)));
                    return null;
                });
            } finally {
                latch.countDown();
            }
        });

        // 트랜잭션 B: +2000원
        executor.execute(() -> {
            try {
                tt.execute(status -> {
                    SellingBid b = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(bidId).orElseThrow();
                    ReflectionTestUtils.setField(b, "price", b.getPrice().add(BigDecimal.valueOf(2000)));
                    return null;
                });
            } finally {
                latch.countDown();
            }
        });

        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // 4. 최종 결과 검증
        // 영속성 컨텍스트를 비우고 DB의 최신값을 다시 읽어옴
        entityManager.clear();
        SellingBid finalBid = sellingBidRepository.findById(bidId).get();

        // 비관적 락이 있다면 순차적으로 처리되어 13000원, 없다면 11000원 혹은 12000원
        assertThat(finalBid.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(13000));
    }
    @Test
    @DisplayName("타임아웃 설정보다 오래 대기하면 예외가 발생해야 한다")
    void pessimisticLock_TimeoutTest() throws InterruptedException {
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 데이터 생성 (앞의 로직과 동일)
        UUID bidId = tt.execute(status -> {
            Brand b = createInstance(Brand.class);
            ReflectionTestUtils.setField(b, "name", "Nike " + UUID.randomUUID());
            entityManager.persist(b);

            Product p = createInstance(Product.class);
            ReflectionTestUtils.setField(p, "name", "Jordan");
            ReflectionTestUtils.setField(p, "brand", b);
            ReflectionTestUtils.setField(p, "category", Category.SHOES);
            entityManager.persist(p);

            ProductOption po = createInstance(ProductOption.class);
            ReflectionTestUtils.setField(po, "product", p);
            ReflectionTestUtils.setField(po, "option", "270");
            entityManager.persist(po);

            SellingBid bid = createInstance(SellingBid.class);
            ReflectionTestUtils.setField(bid, "price", BigDecimal.valueOf(10000));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            ReflectionTestUtils.setField(bid, "productOption", po);
            ReflectionTestUtils.setField(bid, "userId", 1L);

            return sellingBidRepository.save(bid).getId();
        });

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        // Thread A: 락을 잡고 5초 동안 안 놓아줌 (설정된 timeout 3초보다 김)
        new Thread(() -> {
            tt.execute(status -> {
                sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(bidId);
                latch.countDown();
                try { Thread.sleep(5000); } catch (InterruptedException e) {}
                return null;
            });
        }).start();

        latch.await();

        // when & then: Thread B(메인)는 3초를 기다리다 포기하고 예외를 던져야 함
        assertThatThrownBy(() -> {
            tt.execute(status -> {
                return sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(bidId);
            });
        }).satisfies(e -> {
            // DB와 라이브러리에 따라 PessimisticLockingFailureException 또는 QueryTimeoutException이 발생함
            assertThat(e).isInstanceOf(org.springframework.dao.PessimisticLockingFailureException.class);
        });
    }
    @Test
    @DisplayName("쓰기 락이 걸려있어도 일반 조회는 대기 없이 가능해야 한다")
    void pessimisticLock_NonBlockingReadTest() throws InterruptedException {
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        UUID bidId = tt.execute(status -> {
            Brand b = createInstance(Brand.class);
            ReflectionTestUtils.setField(b, "name", "Nike " + UUID.randomUUID());
            entityManager.persist(b);

            Product p = createInstance(Product.class);
            ReflectionTestUtils.setField(p, "name", "Jordan");
            ReflectionTestUtils.setField(p, "brand", b);
            ReflectionTestUtils.setField(p, "category", Category.SHOES);
            entityManager.persist(p);

            ProductOption po = createInstance(ProductOption.class);
            ReflectionTestUtils.setField(po, "product", p);
            ReflectionTestUtils.setField(po, "option", "270");
            entityManager.persist(po);

            SellingBid bid = createInstance(SellingBid.class);
            ReflectionTestUtils.setField(bid, "price", BigDecimal.valueOf(10000));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            ReflectionTestUtils.setField(bid, "productOption", po);
            ReflectionTestUtils.setField(bid, "userId", 1L);

            return sellingBidRepository.save(bid).getId();
        });

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        // Thread A: 수정 목적으로 락 점유
        new Thread(() -> {
            tt.execute(status -> {
                sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(bidId);
                latch.countDown();
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                return null;
            });
        }).start();

        latch.await();

        // when
        long startTime = System.currentTimeMillis();

        Optional<SellingBid> result = sellingBidRepository.findById(bidId);

        long duration = System.currentTimeMillis() - startTime;

        // then
        assertThat(result).isPresent();
        assertThat(duration).isLessThan(500);
    }
}