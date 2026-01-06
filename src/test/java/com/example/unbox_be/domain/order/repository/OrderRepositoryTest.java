package com.example.unbox_be.domain.order.repository;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.config.JpaAuditingConfig;
import com.example.unbox_be.global.config.TestQueryDslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager em;

    private User buyer;
    private User seller;
    private ProductOption productOption;

    @BeforeEach
    void setUp() {
        // 1. User 생성
        buyer = User.createUser("buyer@test.com", "pass", "buyer", "010-1111-1111");
        seller = User.createUser("seller@test.com", "pass", "seller", "010-2222-2222");
        em.persist(buyer);
        em.persist(seller);

        // 2. Product 관련 생성
        Brand brand = Brand.createBrand("Nike", "http://logo.url");
        em.persist(brand);

        Product product = Product.createProduct("Air Force", "model-1", Category.SHOES, "http://img.url", brand);
        em.persist(product);

        productOption = ProductOption.createProductOption(product, "270");
        em.persist(productOption);
        
        em.flush();
        em.clear();
    }

    private Order createOrder(User buyer, User seller, ProductOption option) {
        return Order.builder()
                .sellingBidId(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(150000))
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .receiverAddress("서울시 강남구")
                .receiverZipCode("12345")
                .build();
    }

    @Test
    @DisplayName("주문 저장 및 조회")
    void saveAndFind() {
        // given
        // 영속성 컨텍스트가 초기화되었으므로 merge 필요
        User managedBuyer = em.merge(buyer);
        User managedSeller = em.merge(seller);
        ProductOption managedOption = em.merge(productOption);

        Order order = createOrder(managedBuyer, managedSeller, managedOption);
        
        // when
        Order savedOrder = orderRepository.save(order);
        em.flush();
        em.clear();

        // then
        Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getId()).isEqualTo(savedOrder.getId());
        assertThat(foundOrder.get().getStatus()).isEqualTo(OrderStatus.PENDING_SHIPMENT);
    }

    @Test
    @DisplayName("Soft Delete 동작 확인 - delete() 호출 시 deleted_at 업데이트 및 조회 불가")
    void softDeleteCheck() {
        // given
        User managedBuyer = em.merge(buyer);
        User managedSeller = em.merge(seller);
        ProductOption managedOption = em.merge(productOption);

        Order order = orderRepository.save(createOrder(managedBuyer, managedSeller, managedOption));
        UUID orderId = order.getId();
        em.flush();
        em.clear();

        // when
        Order target = orderRepository.findById(orderId).orElseThrow();
        orderRepository.delete(target); // @SQLDelete 동작
        em.flush();
        em.clear();

        // then
        // 1. 일반 findById 조회 시 결과 없어야 함 (@SQLRestriction)
        Optional<Order> deletedOrder = orderRepository.findById(orderId);
        assertThat(deletedOrder).isEmpty();

        // 2. findByIdAndDeletedAtIsNull 조회 시 결과 없어야 함
        Optional<Order> notFound = orderRepository.findByIdAndDeletedAtIsNull(orderId);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("구매자 ID로 주문 목록 조회 (@EntityGraph 동작 확인)")
    void findAllByBuyerIdAndDeletedAtIsNull() {
        // given
        User managedBuyer = em.merge(buyer);
        User managedSeller = em.merge(seller);
        ProductOption managedOption = em.merge(productOption);

        Order order1 = orderRepository.save(createOrder(managedBuyer, managedSeller, managedOption));
        Order order2 = orderRepository.save(createOrder(managedBuyer, managedSeller, managedOption));
        em.flush();
        em.clear();

        // when
        Page<Order> result = orderRepository.findAllByBuyerIdAndDeletedAtIsNull(managedBuyer.getId(), PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(2);
        
        // EntityGraph 확인: Lazy Loading 없이 ProductOption 등이 로딩되었는지
        Order firstOrder = result.getContent().get(0);
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(firstOrder.getProductOption())).isTrue();
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(firstOrder.getProductOption().getProduct())).isTrue();
    }

    @Test
    @DisplayName("주문 상세 조회 (@EntityGraph 동작 확인 - Seller, Buyer 포함)")
    void findWithDetailsById() {
        // given
        User managedBuyer = em.merge(buyer);
        User managedSeller = em.merge(seller);
        ProductOption managedOption = em.merge(productOption);

        Order order = orderRepository.save(createOrder(managedBuyer, managedSeller, managedOption));
        UUID orderId = order.getId();
        em.flush();
        em.clear();

        // when
        Optional<Order> foundOrder = orderRepository.findWithDetailsById(orderId);

        // then
        assertThat(foundOrder).isPresent();
        Order o = foundOrder.get();
        
        // EntityGraph 확인
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(o.getBuyer())).isTrue();
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(o.getSeller())).isTrue();
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(o.getProductOption())).isTrue();
    }
}
