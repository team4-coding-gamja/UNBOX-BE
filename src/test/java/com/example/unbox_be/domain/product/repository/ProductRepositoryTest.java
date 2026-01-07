package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.global.config.JpaAuditingConfig;
import com.example.unbox_be.global.config.TestQueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager em;

    private Brand nike;
    private Brand adidas;
    private Product p1_NikeShoes;
    private Product p2_AdidasShoes;
    private Product p4_Deleted;

    @BeforeEach
    void setUp() {
        // 1. 브랜드 세팅
        nike = Brand.createBrand("Nike", "http://example.com/logo.png");
        adidas = Brand.createBrand("Adidas", "http://example.com/logo.png");
        em.persist(nike);
        em.persist(adidas);

        // 2. 상품 세팅
        // (1) 나이키 신발 (검색 대상)
        p1_NikeShoes = Product.createProduct("Air Force 1", "AF1-001", Category.SHOES, "http://example.com/image.png",nike);

        // (2) 아디다스 신발
        p2_AdidasShoes = Product.createProduct("Superstar", "S-001", Category.SHOES, "http://example.com/image.png", adidas);

        
        // (4) 삭제된 상품
        p4_Deleted = Product.createProduct(
                "Deleted Item",
                "DEL-001",
                Category.SHOES,
                "http://example.com/image.png",
                nike
        );
        // Soft Delete 처리 (Entity 메서드 사용 가정)
        p4_Deleted.softDelete("test-user");

        em.persist(p1_NikeShoes);
        em.persist(p2_AdidasShoes);
        em.persist(p4_Deleted);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("필터 검색: 브랜드와 카테고리로 필터링하고 삭제된 상품은 제외한다")
    void findByFiltersAndDeletedAtIsNull_BrandAndCategory() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when (나이키 + 신발 검색)
        Page<Product> result = productRepository.findByFiltersAndDeletedAtIsNull(
                nike.getId(),
                Category.SHOES,
                null, // keyword 없음
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Air Force 1");
        // 삭제된 p4는 나이키+신발이지만 나오면 안됨
    }

    @Test
    @DisplayName("키워드 검색: 상품명 또는 모델번호에 키워드가 포함되면 조회된다 (대소문자 무시)")
    void findByFiltersAndDeletedAtIsNull_Keyword() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "force"; // 소문자 검색 -> 'Air Force 1' 매칭 확인

        // when
        Page<Product> result = productRepository.findByFiltersAndDeletedAtIsNull(
                null, null, keyword, pageable
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getModelNumber()).isEqualTo("AF1-001");
    }

    @Test
    @DisplayName("상세 조회: ID로 조회 시 삭제된 상품은 조회되지 않고, Brand 정보가 함께 로드된다")
    void findByIdAndDeletedAtIsNullWithBrandAndDeletedAtIsNull() {
        // given
        UUID activeId = p1_NikeShoes.getId();
        UUID deletedId = p4_Deleted.getId();

        // when
        Optional<Product> activeProduct = productRepository.findByIdAndDeletedAtIsNullWithBrandAndDeletedAtIsNull(activeId);
        Optional<Product> deletedProduct = productRepository.findByIdAndDeletedAtIsNullWithBrandAndDeletedAtIsNull(deletedId);

        // then
        assertThat(activeProduct).isPresent();
        // Brand가 Lazy Loading 없이 로드되었는지 확인 (PersistenceUnitUtil 등으로 검증 가능하나, 여기선 접근 시 예외 없는지로 간접 확인)
        assertThat(activeProduct.get().getBrand().getName()).isEqualTo("Nike");

        assertThat(deletedProduct).isEmpty();
    }

    @Test
    @DisplayName("중복 체크: 본인 ID를 제외하고 같은 모델번호가 있는지 확인한다 (삭제된 것은 제외)")
    void existsByModelNumberAndIdNotAndDeletedAtIsNull() {
        // given
        // p1(AF1-001)은 이미 존재함.

        // when
        // 1. 새로운 상품 생성 시 기존 모델번호(AF1-001) 사용 시도 -> true (중복)
        boolean isDuplicate = productRepository.existsByModelNumberAndIdNotAndDeletedAtIsNull("AF1-001", UUID.randomUUID());

        // 2. p1 본인이 수정 요청하면서 자신의 모델번호(AF1-001) 유지 -> false (중복 아님)
        boolean isSelf = productRepository.existsByModelNumberAndIdNotAndDeletedAtIsNull("AF1-001", p1_NikeShoes.getId());

        // 3. 삭제된 상품의 모델번호(DEL-001) 사용 시도 -> false (삭제되었으므로 재사용 가능)
        boolean isDeletedModel = productRepository.existsByModelNumberAndIdNotAndDeletedAtIsNull("DEL-001", UUID.randomUUID());

        // then
        assertThat(isDuplicate).isTrue();
        assertThat(isSelf).isFalse();
        assertThat(isDeletedModel).isFalse();
    }
}