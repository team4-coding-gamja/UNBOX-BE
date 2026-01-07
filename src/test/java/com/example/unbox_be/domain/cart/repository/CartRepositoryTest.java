package com.example.unbox_be.domain.cart.repository;

import com.example.unbox_be.domain.cart.entity.Cart;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.config.JpaAuditingConfig;
import com.example.unbox_be.global.config.TestQueryDslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private EntityManager em;

    private User user;
    private User otherUser;
    private SellingBid sellingBid;

    @BeforeEach
    void setUp() {
        // 1. Users
        user = User.createUser("user@test.com", "pass", "user", "010-1111-1111");
        otherUser = User.createUser("other@test.com", "pass", "other", "010-2222-2222");
        em.persist(user);
        em.persist(otherUser);

        // 2. Product Env
        Brand brand = Brand.createBrand("Nike", "http://url");
        em.persist(brand);
        Product product = Product.createProduct("Air Force", "model", Category.SHOES, "http://url", brand);
        em.persist(product);
        ProductOption option = ProductOption.createProductOption(product, "270");
        em.persist(option);

        // 3. SellingBid (Seller is otherUser)
        sellingBid = SellingBid.builder()
                .userId(otherUser.getId())
                .productOption(option)
                .price(100000)
                .status(SellingStatus.LIVE)
                .deadline(LocalDateTime.now().plusDays(7))
                .build();
        em.persist(sellingBid);

        em.flush();
        em.clear();
    }

    private Cart createCart(User u, SellingBid bid) {
        Cart c = Cart.builder()
                .user(u)
                .sellingBid(bid)
                .build();
        return cartRepository.save(c);
    }

    // 1. 저장 및 조회
    @Test
    @DisplayName("기본 저장과 조회 확인")
    void saveAndFind() {
        Cart cart = createCart(user, sellingBid);
        em.flush();
        em.clear();

        Cart found = cartRepository.findById(cart.getId()).orElseThrow();
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getSellingBid().getId()).isEqualTo(sellingBid.getId());
    }

    // 2. 중복 체크: 존재함 (True)
    @Test
    @DisplayName("이미 해당 판매 상품이 장바구니에 존재하는 경우 True 반환")
    void exists_True() {
        createCart(user, sellingBid);
        em.flush();
        em.clear();

        boolean exists = cartRepository.existsByUserAndSellingBid(user, sellingBid);
        assertThat(exists).isTrue();
    }

    // 3. 중복 체크: 유저 다름 (False)
    @Test
    @DisplayName("유저가 다른 경우(본인이 아닌 경우)에 False 반환")
    void exists_False_DifferentUser() {
        createCart(user, sellingBid);
        em.flush();
        em.clear();

        boolean exists = cartRepository.existsByUserAndSellingBid(otherUser, sellingBid);
        assertThat(exists).isFalse();
    }

    // 4. 중복 체크: 입찰 다름 (False)
    @Test
    @DisplayName("판매 입찰 정보가 다른 경우에는 False 반환")
    void exists_False_DifferentBid() {
        createCart(user, sellingBid);
        
        // Another Bid
        SellingBid anotherBid = SellingBid.builder()
                .userId(otherUser.getId())
                .productOption(sellingBid.getProductOption())
                .price(200000)
                .status(SellingStatus.LIVE)
                .deadline(LocalDateTime.now())
                .build();
        em.persist(anotherBid);
        em.flush();
        em.clear();

        boolean exists = cartRepository.existsByUserAndSellingBid(user, anotherBid);
        assertThat(exists).isFalse();
    }

    // 5. 내 장바구니 조회 (정렬)
    @Test
    @DisplayName("장바구니 조회할 때, 최신순으로 정렬되는지 확인")
    void getMyCarts_Sorting() {
        // Old Cart (Force update created_at via Native Query)
        Cart oldCart = createCart(user, sellingBid);
        em.flush();
        em.createNativeQuery("UPDATE p_cart SET created_at = ? WHERE id = ?")
                .setParameter(1, LocalDateTime.now().minusDays(1))
                .setParameter(2, oldCart.getId())
                .executeUpdate();

        // New Cart (Use another bid to avoid unique constraints if any - Logic doesn't strictly enforce unique in entity but service does)
        SellingBid newBid = SellingBid.builder()
                .userId(otherUser.getId())
                .productOption(sellingBid.getProductOption())
                .price(120000)
                .status(SellingStatus.LIVE)
                .deadline(LocalDateTime.now())
                .build();
        em.persist(newBid);
        Cart newCart = createCart(user, newBid); // Created Now

        em.flush();
        em.clear();

        List<Cart> result = cartRepository.findAllByUserOrderByCreatedAtDesc(user);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(newCart.getId()); // Newest first
    }

    // 6. Entity Graph 확인
    // findAllByUserOrderByCreatedAtDesc 메서드에는 @EntityGraph가 걸려있어 N+1 문제가 해결되어야 함
    @Test
    @DisplayName("장바구니 조회할 때, Fetch Join으로 연관 엔티티가 로딩되는지 확인")
    void getMyCarts_EntityGraph() {
        createCart(user, sellingBid);
        em.flush();
        em.clear();

        List<Cart> result = cartRepository.findAllByUserOrderByCreatedAtDesc(user);
        Cart cart = result.get(0);

        PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();

        // Check if related entities are loaded (Initialized)
        assertThat(util.isLoaded(cart.getSellingBid())).as("SellingBid should be loaded").isTrue();
        assertThat(util.isLoaded(cart.getSellingBid().getProductOption())).as("ProductOption should be loaded").isTrue();
        assertThat(util.isLoaded(cart.getSellingBid().getProductOption().getProduct())).as("Product should be loaded").isTrue();
        
        // Brand checks requires deeper traversal
        assertThat(util.isLoaded(cart.getSellingBid().getProductOption().getProduct().getBrand())).as("Brand should be loaded").isTrue();
    }

    // 7. 조회 - Deleted 로직 (findByIdAndDeletedAtIsNull)
    @Test
    @DisplayName("삭제되지 않은 건만 조회가 되는지")
    void findById_NotDeleted() {
        Cart cart = createCart(user, sellingBid);
        
        Optional<Cart> result = cartRepository.findByIdAndDeletedAtIsNull(cart.getId());
        assertThat(result).isPresent();
    }

    // 8. 조회 - Deleted 로직 (Soft Delete 후)
    @Test
    @DisplayName("Soft Delete된 건은 조회되지 않는지 확인")
    void findById_AlreadyDeleted() {
        Cart cart = createCart(user, sellingBid);
        cartRepository.delete(cart); // Triggers @SQLDelete
        em.flush();
        em.clear();

        Optional<Cart> result = cartRepository.findByIdAndDeletedAtIsNull(cart.getId());
        assertThat(result).isEmpty();
    }

    // 9. findAllByUser - SQLRestriction 확인
    @Test
    @DisplayName("장바구니 목록을 조회할 때, 삭제된 건은 제외되는지 확인")
    void findAllByUser_Restriction() {
        Cart c1 = createCart(user, sellingBid);
        
        // Use different bid for c2 to avoid potential Unique Constraint violation
        SellingBid newBid = SellingBid.builder()
                .userId(otherUser.getId())
                .productOption(sellingBid.getProductOption())
                .price(200000)
                .status(SellingStatus.LIVE)
                .deadline(LocalDateTime.now())
                .build();
        em.persist(newBid);
        
        Cart c2 = createCart(user, newBid);
        
        cartRepository.delete(c1); // Soft Delete c1
        em.flush();
        em.clear();

        List<Cart> result = cartRepository.findAllByUser(user);
        
        // c1 should be hidden
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(c2.getId());
    }

    // 10. findAllByUser - 아무것도 없을 때
    @Test
    @DisplayName("장바구니 목록을 조회할 경우 아무 상품도 없을 때")
    void findAllByUser_Empty() {
        List<Cart> result = cartRepository.findAllByUser(user);
        assertThat(result).isEmpty();
    }
}
