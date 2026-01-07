package com.example.unbox_be.domain.wishlist.repository;

import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.wishlist.entity.Wishlist;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
class WishlistRepositoryTest {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private EntityManager em;

    private User user;
    private User otherUser;
    private ProductOption option1;
    private ProductOption option2;

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
        
        option1 = ProductOption.createProductOption(product, "270");
        option2 = ProductOption.createProductOption(product, "280");
        em.persist(option1);
        em.persist(option2);

        em.flush();
        em.clear();
    }

    private Wishlist createWishlist(User u, ProductOption option) {
        Wishlist w = Wishlist.builder()
                .user(u)
                .productOption(option)
                .build();
        return wishlistRepository.save(w);
    }

    @Test
    @DisplayName("위시리스트 저장 및 조회 성공 확인")
    void saveAndFind() {
        // given
        Wishlist wishlist = createWishlist(user, option1);
        em.flush();
        em.clear();

        // when
        Wishlist found = wishlistRepository.findById(wishlist.getId()).orElseThrow();

        // then
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getProductOption().getId()).isEqualTo(option1.getId());
    }

    @Test
    @DisplayName("특정 유저가 특정 상품 옵션을 이미 찜했는지 확인 (True)")
    void exists_True() {
        // given
        createWishlist(user, option1);
        em.flush();
        em.clear();

        // when
        boolean exists = wishlistRepository.existsByUserAndProductOptionAndDeletedAtIsNull(user, option1);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("유저가 다르거나 상품이 다른 경우 중복 체크 (False)")
    void exists_False() {
        // given
        createWishlist(user, option1);
        em.flush();
        em.clear();

        // when & then
        // 1. 다른 유저
        boolean existsUser = wishlistRepository.existsByUserAndProductOptionAndDeletedAtIsNull(otherUser, option1);
        assertThat(existsUser).isFalse();

        // 2. 다른 상품 옵션
        boolean existsOption = wishlistRepository.existsByUserAndProductOptionAndDeletedAtIsNull(user, option2);
        assertThat(existsOption).isFalse();
    }

    @Test
    @DisplayName("내 위시리스트 목록 조회 시 최신순 정렬 확인")
    void getMyWishlist_Sorting() {
        // given
        Wishlist oldWish = createWishlist(user, option1);
        em.flush();
        // createAt 강제 수정 (과거)
        em.createNativeQuery("UPDATE p_wishlists SET created_at = ? WHERE wishlist_id = ?")
                .setParameter(1, LocalDateTime.now().minusDays(1))
                .setParameter(2, oldWish.getId())
                .executeUpdate();

        Wishlist newWish = createWishlist(user, option2); // 현재 시점 생성
        em.flush();
        em.clear();

        // when
        Slice<Wishlist> result = wishlistRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(newWish.getId());
        assertThat(result.getContent().get(1).getId()).isEqualTo(oldWish.getId());
    }

    @Test
    @DisplayName("내 위시리스트 조회 시 연관 엔티티(Product, Option) Fetch Join 여부 확인")
    void getMyWishlist_EntityGraph() {
        // given
        createWishlist(user, option1);
        em.flush();
        em.clear();

        // when
        Slice<Wishlist> result = wishlistRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, PageRequest.of(0, 10));
        Wishlist wishlist = result.getContent().get(0);

        PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();

        // then
        assertThat(util.isLoaded(wishlist.getProductOption())).as("ProductOption should be loaded").isTrue();
        assertThat(util.isLoaded(wishlist.getProductOption().getProduct())).as("Product should be loaded").isTrue();
    }

    @Test
    @DisplayName("삭제되지 않은 위시리스트 단건 조회 성공")
    void findById_NotDeleted() {
        // given
        Wishlist wishlist = createWishlist(user, option1);
        em.flush();
        em.clear();

        // when
        Optional<Wishlist> result = wishlistRepository.findByIdAndDeletedAtIsNull(wishlist.getId());

        // then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Soft Delete된 위시리스트는 조회되지 않아야 함")
    void findById_AlreadyDeleted() {
        // given
        Wishlist wishlist = createWishlist(user, option1);
        wishlistRepository.delete(wishlist); // Soft Delete
        em.flush();
        em.clear();

        // when
        Optional<Wishlist> result = wishlistRepository.findByIdAndDeletedAtIsNull(wishlist.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("목록 조회 시 삭제된 위시리스트는 제외되어야 함")
    void findByUser_ExcludeDeleted() {
        // given
        Wishlist deletedWish = createWishlist(user, option1);
        Wishlist activeWish = createWishlist(user, option2);
        
        wishlistRepository.delete(deletedWish);
        em.flush();
        em.clear();

        // when
        Slice<Wishlist> result = wishlistRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(activeWish.getId());
    }
}
