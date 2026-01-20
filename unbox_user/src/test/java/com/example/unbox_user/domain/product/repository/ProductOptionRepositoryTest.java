//package com.example.unbox_be.domain.product.repository;
//
//import com.example.unbox_be.domain.product.entity.Brand;
//import com.example.unbox_be.domain.product.entity.Category;
//import com.example.unbox_be.domain.product.entity.Product;
//import com.example.unbox_be.domain.product.entity.ProductOption;
//import com.example.unbox_be.global.config.JpaAuditingConfig;
//import com.example.unbox_be.global.config.TestQueryDslConfig;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.context.annotation.Import;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.test.context.TestPropertySource;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
//@TestPropertySource(properties = {"spring.sql.init.mode=never"})
//@DataJpaTest
//class ProductOptionRepositoryTest {
//
//    @Autowired
//    private ProductOptionRepository productOptionRepository;
//
//    @Autowired
//    private TestEntityManager em;
//
//    private Product product;
//    private Brand brand;
//
//    @BeforeEach
//    void setUp() {
//        // 1. 브랜드 생성 및 저장 (Product의 연관관계 주인)
//        brand = Brand.createBrand("Nike", "http://example.com/logo.png");
//        em.persist(brand);
//
//        // 2. 상품 생성 및 저장
//        product = Product.createProduct(
//                "Air Force 1",
//                "AF1-001",
//                Category.SHOES,
//                "http://example.com/image.png",
//                brand
//        );
//        em.persist(product);
//    }
//
//    @Test
//    @DisplayName("ID로 조회 시 deletedAt이 null인(삭제되지 않은) 옵션만 조회된다")
//    void findByIdAndDeletedAtIsNull() {
//        // given
//        ProductOption activeOption = createOption(product, "260", null);
//        ProductOption deletedOption = createOption(product, "270", LocalDateTime.now());
//
//        // when
//        Optional<ProductOption> foundActive = productOptionRepository.findByIdAndDeletedAtIsNull(activeOption.getId());
//        Optional<ProductOption> foundDeleted = productOptionRepository.findByIdAndDeletedAtIsNull(deletedOption.getId());
//
//        // then
//        assertThat(foundActive).isPresent();
//        assertThat(foundActive.get().getOption()).isEqualTo("260");
//
//        assertThat(foundDeleted).isEmpty(); // 삭제된 데이터는 조회되면 안 됨
//    }
//
//    @Test
//    @DisplayName("특정 상품의 삭제되지 않은 옵션 목록을 모두 조회한다")
//    void findAllByProductIdAndDeletedAtIsNull() {
//        // given
//        createOption(product, "230", null);
//        createOption(product, "240", null);
//        createOption(product, "250", LocalDateTime.now()); // 삭제됨
//
//        em.flush();
//        em.clear();
//
//        // when
//        List<ProductOption> options = productOptionRepository.findAllByProductIdAndDeletedAtIsNull(product.getId());
//
//        // then
//        assertThat(options).hasSize(2); // 3개 중 1개 삭제 -> 2개
//        assertThat(options).extracting("option")
//                .containsExactlyInAnyOrder("230", "240");
//    }
//
//    @Test
//    @DisplayName("여러 상품 ID 목록(Bulk)으로 옵션을 한 번에 조회한다")
//    void findAllByProductIdInAndDeletedAtIsNull() {
//        // given
//        Product otherProduct = Product.createProduct(
//                "Jordan 1",
//                "JD1-001",
//                Category.SHOES,
//                "http://example.com/jordan.png",
//                brand
//        );
//        em.persist(otherProduct);
//
//        createOption(product, "260", null);      // product 옵션
//        createOption(otherProduct, "270", null); // otherProduct 옵션
//        createOption(product, "280", LocalDateTime.now()); // 삭제된 옵션
//
//        // when
//        List<ProductOption> results = productOptionRepository.findAllByProductIdInAndDeletedAtIsNull(
//                List.of(product.getId(), otherProduct.getId())
//        );
//
//        // then
//        assertThat(results).hasSize(2);
//        assertThat(results).extracting("option")
//                .contains("260", "270");
//    }
//
//    @Test
//    @DisplayName("특정 상품에 해당 옵션이 이미 존재하는지 중복 체크한다 (삭제된 것 제외)")
//    void existsByProductAndOptionAndDeletedAtIsNull() {
//        // given
//        createOption(product, "260", null);
//        createOption(product, "270", LocalDateTime.now()); // 삭제됨
//
//        // when
//        boolean existsActive = productOptionRepository.existsByProductAndOptionAndDeletedAtIsNull(product, "260");
//        boolean existsDeleted = productOptionRepository.existsByProductAndOptionAndDeletedAtIsNull(product, "270");
//        boolean existsNew = productOptionRepository.existsByProductAndOptionAndDeletedAtIsNull(product, "280");
//
//        // then
//        assertThat(existsActive).isTrue();
//        assertThat(existsDeleted).isFalse(); // 삭제된 건 중복 아님 (다시 생성 가능)
//        assertThat(existsNew).isFalse();
//    }
//
//    @Test
//    @DisplayName("특정 상품의 옵션 목록을 페이징하여 조회한다")
//    void findByProductIdAndDeletedAtIsNull_Paging() {
//        // given
//        for (int i = 0; i < 10; i++) {
//            createOption(product, "Size-" + i, null);
//        }
//
//        // when (Page 0, Size 5)
//        Pageable pageable = PageRequest.of(0, 5);
//        Page<ProductOption> pageResult = productOptionRepository.findByProductIdAndDeletedAtIsNull(product.getId(), pageable);
//
//        // then
//        assertThat(pageResult.getContent()).hasSize(5); // 조회된 개수
//        assertThat(pageResult.getTotalElements()).isEqualTo(10); // 전체 개수
//        assertThat(pageResult.getTotalPages()).isEqualTo(2); // 전체 페이지 수
//        assertThat(pageResult.getNumber()).isEqualTo(0); // 현재 페이지 인덱스
//    }
//
//    // --- Helper Method ---
//    private ProductOption createOption(Product product, String optionName, LocalDateTime deletedAt) {
//        ProductOption option = ProductOption.createProductOption(product, optionName);
//
//        // deletedAt은 Setter나 별도 메서드로 설정한다고 가정 (Entity 내부 구현에 따라 조정 필요)
//        if (deletedAt != null) {
//            option.softDelete("test-user"); // 또는 option.setDeletedAt(deletedAt);
//        }
//
//        return em.persist(option);
//    }
//}