//package com.example.unbox_be.domain.order.repository;
//
//import com.example.unbox_be.domain.order.entity.Order;
//import com.example.unbox_be.domain.order.entity.OrderStatus;
//import com.example.unbox_be.domain.product.entity.Brand;
//import com.example.unbox_be.domain.product.entity.Category;
//import com.example.unbox_be.domain.product.entity.Product;
//import com.example.unbox_be.domain.product.entity.ProductOption;
//import com.example.unbox_be.domain.user.entity.User;
//import com.example.unbox_be.global.config.JpaAuditingConfig;
//import com.example.unbox_be.global.config.TestQueryDslConfig;
//import jakarta.persistence.EntityManager;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//
//import java.math.BigDecimal;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//
//@DataJpaTest
//@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
//@ActiveProfiles("test")
//@TestPropertySource(properties = {"spring.sql.init.mode=never"})
//class OrderRepositoryTest {
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @Autowired
//    private EntityManager em;
//
//    private User buyer;
//    private User seller;
//    private ProductOption productOption;
//    private Product product; // Added to access fields for snapshot
//    private Brand brand;     // Added to access fields for snapshot
//
//    @BeforeEach
//    void setUp() {
//        // 1. User 생성
//        buyer = User.createUser("buyer@test.com", "pass", "buyer", "010-1111-1111");
//        seller = User.createUser("seller@test.com", "pass", "seller", "010-2222-2222");
//        em.persist(buyer);
//        em.persist(seller);
//
//        // 2. Product 관련 생성
//        brand = Brand.createBrand("Nike", "http://logo.url");
//        em.persist(brand);
//
//        product = Product.createProduct("Air Force", "model-1", Category.SHOES, "http://img.url", brand);
//        em.persist(product);
//
//        productOption = ProductOption.createProductOption(product, "270");
//        em.persist(productOption);
//
//        em.flush();
//        em.clear();
//    }
//
//    private Order createOrder(User buyer, User seller, ProductOption option) {
//        // Retrieve Product/Brand info from the option for snapshotting
//        // Note: In real service, this comes from ProductClient. Here we just use the entity data.
//        // Since em.clear() was called, we might need to re-fetch if lazy loading,
//        // but since we pass managedOption or we just use IDs, let's just use the fields.
//
//        return Order.builder()
//                .sellingBidId(UUID.randomUUID())
//                .buyer(buyer)
//                .seller(seller)
//                .productOptionId(option.getId())
//                .productId(option.getProduct().getId()) // Assuming accessible or just use mock ID for test
//                .productName("Air Force")     // Snapshot
//                .modelNumber("model-1")       // Snapshot
//                .optionName("270")            // Snapshot
//                .imageUrl("http://img.url")   // Snapshot
//                .brandName("Nike")            // Snapshot
//                .price(BigDecimal.valueOf(150000))
//                .receiverName("홍길동")
//                .receiverPhone("010-1234-5678")
//                .receiverAddress("서울시 강남구")
//                .receiverZipCode("12345")
//                .build();
//    }
//
//    @Test
//    @DisplayName("주문 저장 및 조회")
//    void saveAndFind() {
//        // given
//        User managedBuyer = em.merge(buyer);
//        User managedSeller = em.merge(seller);
//        ProductOption managedOption = em.merge(productOption);
//
//        Order order = createOrder(managedBuyer, managedSeller, managedOption);
//
//        // when
//        Order savedOrder = orderRepository.save(order);
//        em.flush();
//        em.clear();
//
//        // then
//        Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());
//        assertThat(foundOrder).isPresent();
//        assertThat(foundOrder.get().getId()).isEqualTo(savedOrder.getId());
//        assertThat(foundOrder.get().getStatus()).isEqualTo(OrderStatus.PENDING_SHIPMENT);
//        // Verify snapshot field
//        assertThat(foundOrder.get().getProductName()).isEqualTo("Air Force");
//    }
//
//    @Test
//    @DisplayName("Soft Delete 동작이 잘 되는지 확인")
//    void softDeleteCheck() {
//        // given
//        User managedBuyer = em.merge(buyer);
//        User managedSeller = em.merge(seller);
//        ProductOption managedOption = em.merge(productOption);
//
//        Order order = orderRepository.save(createOrder(managedBuyer, managedSeller, managedOption));
//        UUID orderId = order.getId();
//        em.flush();
//        em.clear();
//
//        // when
//        Order target = orderRepository.findById(orderId).orElseThrow();
//        orderRepository.delete(target); // @SQLDelete 동작
//        em.flush();
//        em.clear();
//
//        // then
//        // 1. 일반 findById 조회 시 결과 없어야 함 (@SQLRestriction)
//        Optional<Order> deletedOrder = orderRepository.findById(orderId);
//        assertThat(deletedOrder).isEmpty();
//
//        // 2. findByIdAndDeletedAtIsNull 조회 시 결과 없어야 함
//        Optional<Order> notFound = orderRepository.findByIdAndDeletedAtIsNull(orderId);
//        assertThat(notFound).isEmpty();
//    }
//
//    @Test
//    @DisplayName("구매자 ID로 주문 목록 조회")
//    void findAllByBuyerIdAndDeletedAtIsNull() {
//        // given
//        User managedBuyer = em.merge(buyer);
//        User managedSeller = em.merge(seller);
//        ProductOption managedOption = em.merge(productOption);
//
//        Order order1 = orderRepository.save(createOrder(managedBuyer, managedSeller, managedOption));
//        Order order2 = orderRepository.save(createOrder(managedBuyer, managedSeller, managedOption));
//        em.flush();
//        em.clear();
//
//        // when
//        Page<Order> result = orderRepository.findAllByBuyerIdAndDeletedAtIsNull(managedBuyer.getId(), PageRequest.of(0, 10));
//
//        // then
//        assertThat(result.getContent()).hasSize(2);
//
//        // [Refactored] No direct relation to ProductOption anymore, so no need to check isLoaded(productOption)
//        // Instead, we can check if snapshot data is there.
//        Order firstOrder = result.getContent().get(0);
//        assertThat(firstOrder.getProductName()).isEqualTo("Air Force");
//    }
//
//    @Test
//    @DisplayName("주문 상세 조회 (Seller, Buyer 포함해서 가져올 때)")
//    void findWithDetailsById() {
//        // given
//        User managedBuyer = em.merge(buyer);
//        User managedSeller = em.merge(seller);
//        ProductOption managedOption = em.merge(productOption);
//
//        Order order = orderRepository.save(createOrder(managedBuyer, managedSeller, managedOption));
//        UUID orderId = order.getId();
//        em.flush();
//        em.clear();
//
//        // when
//        Optional<Order> foundOrder = orderRepository.findWithDetailsById(orderId);
//
//        // then
//        assertThat(foundOrder).isPresent();
//        Order o = foundOrder.get();
//
//        // EntityGraph 확인
//        // [Refactored] Order -> ProductOption relation removed from EntityGraph. Only Buyer/Seller remain.
//        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(o.getBuyer())).isTrue();
//        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(o.getSeller())).isTrue();
//    }
//}
