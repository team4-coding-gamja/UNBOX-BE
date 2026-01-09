package com.example.unbox_be.domain.admin.order.repository;

import com.example.unbox_be.domain.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.AdminOrderRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.config.JpaAuditingConfig;
import com.example.unbox_be.global.config.TestQueryDslConfig;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
class AdminOrderRepositoryTest {

    @Autowired
    private AdminOrderRepository adminOrderRepository;

    @Autowired
    private EntityManager em;

    private User buyer;
    private User seller;
    private Brand brandNike;
    private Brand brandAdidas;
    private Product productAirForce;
    private Product productSuperstar;

    @BeforeEach
    void setUp() {
        // 1. User
        buyer = User.createUser("buyer@test.com", "pass", "buyer", "010-1111-1111");
        seller = User.createUser("seller@test.com", "pass", "seller", "010-2222-2222");
        em.persist(buyer);
        em.persist(seller);

        // 2. Brand
        brandNike = Brand.createBrand("Nike", "http://url");
        brandAdidas = Brand.createBrand("Adidas", "http://url");
        em.persist(brandNike);
        em.persist(brandAdidas);

        // 3. Product
        productAirForce = Product.createProduct("Air Force", "model-1", Category.SHOES, "http://url", brandNike);
        productSuperstar = Product.createProduct("Superstar", "model-2", Category.SHOES, "http://url", brandAdidas);
        em.persist(productAirForce);
        em.persist(productSuperstar);
    }

    private Order createOrder(User buyer, User seller, Product product, OrderStatus status) {
        ProductOption option = ProductOption.createProductOption(product, "270");
        em.persist(option); // Option은 매번 생성 (Cascade 설정 없다고 가정)

        Order order = Order.builder()
                .sellingBidId(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(100000))
                .receiverName("receiver")
                .receiverPhone("010-0000-0000")
                .receiverAddress("Seoul")
                .receiverZipCode("12345")
                .build();
        
        // 상태 변경 (Reflection 없이 Setter없다면 로직상 처음에 PENDING_SHIPMENT임. 여기선 테스트를 위해 상태별로 생성 필요하면 변경)
        // Order 생성 시점엔 PENDING_SHIPMENT. 
        // 만약 다른 상태 테스트하려면 reflection 사용하거나 상태 변경 메서드 호출 (여기선 상태 변경 메서드가 있다고 가정하거나 Reflection 사용)
        // 편의상 Reflection 없이 생성 후 상태 변경 메서드 호출 or 별도 로직. 
        // 하지만 Order 객체는 Setter가 없으므로 updateAdminStatus 등을 써야 함.
        // 테스트 편의를 위해 일단 생성 후, em.flush() 후에 다시 조회해서 값을 바꿀 수는 없으니...
        // 아, Order 엔티티에 상태 변경 메서드가 있다면 그걸 씁니다.
        // 여기선 간단하게 테스트용으로 생성 후 Reflection으로 상태 강제 주입하겠습니다. (테스트 목적)
        
        org.springframework.test.util.ReflectionTestUtils.setField(order, "status", status);
        return order;
    }

    @Test
    @DisplayName("관리자 주문 검색 - 전체 조회")
    void searchAll() {
        // given
        Order order1 = createOrder(buyer, seller, productAirForce, OrderStatus.PENDING_SHIPMENT);
        Order order2 = createOrder(buyer, seller, productSuperstar, OrderStatus.DELIVERED);
        adminOrderRepository.save(order1);
        adminOrderRepository.save(order2);
        
        // created_at 정렬을 위해 시간차 필요하면 flush
        em.flush();
        em.clear();

        // when
        OrderSearchCondition condition = new OrderSearchCondition(); // 모든 조건 null
        Page<Order> result = adminOrderRepository.findAdminOrders(condition, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("관리자 주문 검색 - 상태 필터링")
    void searchByStatus() {
        // given
        Order order1 = createOrder(buyer, seller, productAirForce, OrderStatus.PENDING_SHIPMENT);
        Order order2 = createOrder(buyer, seller, productSuperstar, OrderStatus.DELIVERED);
        adminOrderRepository.save(order1);
        adminOrderRepository.save(order2);
        em.flush();
        em.clear();

        // when
        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setStatus(OrderStatus.DELIVERED);
        
        Page<Order> result = adminOrderRepository.findAdminOrders(condition, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("관리자 주문 검색 - 상품명 포함")
    void searchByProductName() {
        // given
        Order order1 = createOrder(buyer, seller, productAirForce, OrderStatus.PENDING_SHIPMENT); // "Air Force"
        Order order2 = createOrder(buyer, seller, productSuperstar, OrderStatus.PENDING_SHIPMENT); // "Superstar"
        adminOrderRepository.save(order1);
        adminOrderRepository.save(order2);
        em.flush();
        em.clear();

        // when
        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setProductName("Force");

        Page<Order> result = adminOrderRepository.findAdminOrders(condition, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductOption().getProduct().getName()).isEqualTo("Air Force");
    }

    @Test
    @DisplayName("관리자 주문 검색 - 브랜드명 포함 (Join 확인)")
    void searchByBrandName() {
         // given
        Order order1 = createOrder(buyer, seller, productAirForce, OrderStatus.PENDING_SHIPMENT); // Nike
        Order order2 = createOrder(buyer, seller, productSuperstar, OrderStatus.PENDING_SHIPMENT); // Adidas
        adminOrderRepository.save(order1);
        adminOrderRepository.save(order2);
        em.flush();
        em.clear();

        // when
        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setBrandName("Nike");

        Page<Order> result = adminOrderRepository.findAdminOrders(condition, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductOption().getProduct().getBrand().getName()).isEqualTo("Nike");
    }

    @Test
    @DisplayName("관리자 주문 검색 - 기간 조회")
    void searchByPeriod() {
        // given
        Order order1 = createOrder(buyer, seller, productAirForce, OrderStatus.PENDING_SHIPMENT);
        adminOrderRepository.save(order1);
        em.flush();

        // Auditing 기능으로 인해 save 시점에 현재 시간으로 덮어씌워지므로, Native Query로 강제 업데이트
        em.createNativeQuery("UPDATE p_orders SET created_at = ? WHERE order_id = ?")
                .setParameter(1, LocalDate.now().minusDays(5).atStartOfDay())
                .setParameter(2, order1.getId())
                .executeUpdate();

        Order order2 = createOrder(buyer, seller, productSuperstar, OrderStatus.PENDING_SHIPMENT);
        // order2는 오늘 날짜 (default)
        adminOrderRepository.save(order2);
        
        em.flush();
        em.clear();

        // when: 어제 ~ 내일 검색 (order2만 걸려야 함)
        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setStartDate(LocalDate.now().minusDays(1));
        condition.setEndDate(LocalDate.now().plusDays(1));

        Page<Order> result = adminOrderRepository.findAdminOrders(condition, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        // order2인지 확인 (ID 비교 등, DB 재조회라 ID 모르면 검증 어려우나 size로 1개 확인됨)
    }
}
