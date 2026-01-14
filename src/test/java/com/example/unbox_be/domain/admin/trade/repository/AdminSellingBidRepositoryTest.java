//package com.example.unbox_be.domain.admin.trade.repository;
//
//import com.example.unbox_be.domain.trade.dto.request.SellingBidSearchCondition;
//import com.example.unbox_be.domain.product.entity.Brand;
//import com.example.unbox_be.domain.product.entity.Category;
//import com.example.unbox_be.domain.product.entity.Product;
//import com.example.unbox_be.domain.product.entity.ProductOption;
//import com.example.unbox_be.domain.trade.entity.SellingBid;
//import com.example.unbox_be.domain.trade.entity.SellingStatus;
//import com.example.unbox_be.domain.trade.repository.AdminSellingBidRepository;
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
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
//@ActiveProfiles("test")
//@TestPropertySource(properties = {"spring.sql.init.mode=never"})
//class AdminSellingBidRepositoryTest {
//
//    @Autowired
//    private AdminSellingBidRepository repository;
//
//    @Autowired
//    private EntityManager em;
//
//    private User seller;
//    private Brand brandNike;
//    private Brand brandAdidas;
//    private Product productAirForce;
//    private Product productSuperstar;
//
//    @BeforeEach
//    void setUp() {
//        seller = User.createUser("seller@test.com", "pass", "seller", "010-2222-2222");
//        em.persist(seller);
//
//        brandNike = Brand.createBrand("Nike", "http://url");
//        brandAdidas = Brand.createBrand("Adidas", "http://url");
//        em.persist(brandNike);
//        em.persist(brandAdidas);
//
//        productAirForce = Product.createProduct("Air Force", "model-1", Category.SHOES, "http://url", brandNike);
//        productSuperstar = Product.createProduct("Superstar", "model-2", Category.SHOES, "http://url", brandAdidas);
//        em.persist(productAirForce);
//        em.persist(productSuperstar);
//    }
//
//    private SellingBid create(User seller, Product product, SellingStatus status) {
//        ProductOption option = ProductOption.createProductOption(product, "270");
//        em.persist(option);
//        return SellingBid.builder()
//                .userId(seller.getId())
//                .productOption(option)
//                .price(BigDecimal.valueOf(100000))
//                .status(status)
//                .deadline(LocalDateTime.now().plusDays(7))
//                .build();
//    }
//
//    private void saveAndSetCreatedAt(SellingBid bid, LocalDateTime dateTime) {
//        repository.save(bid);
//        em.flush();
//        em.createNativeQuery("UPDATE p_selling_bids SET created_at = ? WHERE selling_id = ?")
//                .setParameter(1, dateTime)
//                .setParameter(2, bid.getId())
//                .executeUpdate();
//    }
//
//    // 1. 전체 조회
//    @Test
//    @DisplayName("검색 조건 없이 전체 조회")
//    void searchAll() {
//        repository.save(create(seller, productAirForce, SellingStatus.LIVE));
//        repository.save(create(seller, productSuperstar, SellingStatus.MATCHED));
//
//        Page<SellingBid> result = repository.findAdminSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(2);
//    }
//
//    // 2. 단일 조건 검색: 상태 (LIVE)
//    @Test
//    @DisplayName("상태 검색: LIVE")
//    void searchByStatus_Live() {
//        repository.save(create(seller, productAirForce, SellingStatus.LIVE));
//        repository.save(create(seller, productSuperstar, SellingStatus.MATCHED));
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setStatus(SellingStatus.LIVE);
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).getStatus()).isEqualTo(SellingStatus.LIVE);
//    }
//
//    // 3. 단일 조건 검색: 상태 (MATCHED)
//    @Test
//    @DisplayName("상태 검색: MATCHED")
//    void searchByStatus_Matched() {
//        repository.save(create(seller, productAirForce, SellingStatus.LIVE));
//        repository.save(create(seller, productSuperstar, SellingStatus.MATCHED));
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setStatus(SellingStatus.MATCHED);
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).getStatus()).isEqualTo(SellingStatus.MATCHED);
//    }
//
//    // 4. 단일 조건 검색: 상품명 (전체 일치)
//    @Test
//    @DisplayName("상품명 검색: 전체 문자열 일치")
//    void searchByProductName_Full() {
//        repository.save(create(seller, productAirForce, SellingStatus.LIVE)); // "Air Force"
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setProductName("Air Force");
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//    }
//
//    // 5. 단일 조건 검색: 상품명 (부분 일치)
//    @Test
//    @DisplayName("상품명 검색: 부분 문자열 일치")
//    void searchByProductName_Partial() {
//        repository.save(create(seller, productAirForce, SellingStatus.LIVE)); // "Air Force"
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setProductName("Force");
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//    }
//
//    // 6. 단일 조건 검색: 브랜드명 (IgnoreCase)
//    @Test
//    @DisplayName("브랜드명 검색: 대소문자 무시")
//    void searchByBrandName_IgnoreCase() {
//        repository.save(create(seller, productAirForce, SellingStatus.LIVE)); // "Nike"
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setBrandName("nike");
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//    }
//
//    // 7. 기간 검색: 시작일만 지정
//    @Test
//    @DisplayName("기간 검색: 시작일 이후(Goe) 조회")
//    void searchByStartDateOnly() {
//        SellingBid bid = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(bid, LocalDate.now().minusDays(1).atStartOfDay()); // 어제
//
//        SellingBid oldBid = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(oldBid, LocalDate.now().minusDays(5).atStartOfDay()); // 5일전
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setStartDate(LocalDate.now().minusDays(2)); // 2일전 이후 검색
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).getId()).isEqualTo(bid.getId());
//    }
//
//    // 8. 기간 검색: 종료일만 지정
//    @Test
//    @DisplayName("기간 검색: 종료일 이전(Lt) 조회")
//    void searchByEndDateOnly() {
//        SellingBid bid = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(bid, LocalDate.now().minusDays(3).atStartOfDay()); // 3일전
//
//        SellingBid recentBid = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(recentBid, LocalDate.now().atStartOfDay()); // 오늘
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setEndDate(LocalDate.now().minusDays(1)); // 어제까지 검색
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).getId()).isEqualTo(bid.getId());
//    }
//
//    // 9. 기간 검색: 범위 (Between)
//    @Test
//    @DisplayName("기간 검색: 시작일 ~ 종료일 범위 조회")
//    void searchByPeriod_Between() {
//        SellingBid target = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(target, LocalDate.now().minusDays(2).atStartOfDay());
//
//        saveAndSetCreatedAt(create(seller, productAirForce, SellingStatus.LIVE), LocalDate.now().minusDays(10).atStartOfDay()); // 과거
//        saveAndSetCreatedAt(create(seller, productAirForce, SellingStatus.LIVE), LocalDate.now().plusDays(1).atStartOfDay()); // 확실한 미래 (범위 밖)
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setStartDate(LocalDate.now().minusDays(3));
//        cond.setEndDate(LocalDate.now().minusDays(1));
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).getId()).isEqualTo(target.getId());
//    }
//
//    // 10. 기간 검색: 경계값 (시작일 00:00:00)
//    @Test
//    @DisplayName("기간 검색: 시작일 00시 00분 데이터 포함 확인")
//    void searchPeriod_StartBoundary() {
//        SellingBid bid = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(bid, LocalDate.now().atTime(0, 0, 0));
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setStartDate(LocalDate.now());
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).isNotEmpty();
//    }
//
//    // 11. 기간 검색: 경계값 (종료일 23:59:59)
//    @Test
//    @DisplayName("기간 검색: 종료일 23시 59분 데이터 포함 확인")
//    void searchPeriod_EndBoundary() {
//        SellingBid bid = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(bid, LocalDate.now().atTime(23, 59, 59));
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setEndDate(LocalDate.now()); // 종료일은 해당일의 마지막까지 포함됨
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).isNotEmpty();
//    }
//
//    // 12. 복합 조건: 상태 + 상품명
//    @Test
//    @DisplayName("복합 조건: 상태와 상품명이 모두 일치해야 함")
//    void searchComplex_StatusAndProduct() {
//        repository.save(create(seller, productAirForce, SellingStatus.LIVE)); // Target
//        repository.save(create(seller, productAirForce, SellingStatus.MATCHED)); // Status Fail
//        repository.save(create(seller, productSuperstar, SellingStatus.LIVE)); // Product Fail
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setStatus(SellingStatus.LIVE);
//        cond.setProductName("Air Force");
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//    }
//
//    // 13. 복합 조건: 브랜드 + 기간
//    @Test
//    @DisplayName("복합 조건: 브랜드와 기간이 모두 일치해야 함")
//    void searchComplex_BrandAndPeriod() {
//        SellingBid target = create(seller, productAirForce, SellingStatus.LIVE); // Nike
//        saveAndSetCreatedAt(target, LocalDate.now().minusDays(1).atStartOfDay());
//
//        SellingBid wrongBrand = create(seller, productSuperstar, SellingStatus.LIVE); // Adidas
//        saveAndSetCreatedAt(wrongBrand, LocalDate.now().minusDays(1).atStartOfDay());
//
//        SellingBid wrongDate = create(seller, productAirForce, SellingStatus.LIVE); // Nike
//        saveAndSetCreatedAt(wrongDate, LocalDate.now().minusDays(5).atStartOfDay());
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setBrandName("Nike");
//        cond.setStartDate(LocalDate.now().minusDays(2));
//        cond.setEndDate(LocalDate.now());
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//    }
//
//    // 14. 모든 검색 조건 포함
//    @Test
//    @DisplayName("모든 검색 조건 적용 (상태+상품+브랜드+기간)")
//    void searchAllConditions() {
//        SellingBid target = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(target, LocalDate.now().minusDays(1).atStartOfDay());
//
//        // Noise data...
//        repository.save(create(seller, productAirForce, SellingStatus.MATCHED));
//        repository.save(create(seller, productSuperstar, SellingStatus.LIVE));
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setStatus(SellingStatus.LIVE);
//        cond.setProductName("Air");
//        cond.setBrandName("Nike");
//        cond.setStartDate(LocalDate.now().minusDays(2));
//        cond.setEndDate(LocalDate.now());
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).hasSize(1);
//    }
//
//    // 15. 결과 없음
//    @Test
//    @DisplayName("조건에 맞는 결과가 없을 때 빈 페이지 반환")
//    void searchNoResult() {
//        repository.save(create(seller, productAirForce, SellingStatus.LIVE));
//
//        SellingBidSearchCondition cond = new SellingBidSearchCondition();
//        cond.setProductName("NonExistingProduct");
//
//        Page<SellingBid> result = repository.findAdminSellingBids(cond, PageRequest.of(0, 10));
//        assertThat(result.getContent()).isEmpty();
//    }
//
//    // 16. Soft Delete 확인
//    @Test
//    @DisplayName("Soft Delete된 데이터는 조회되지 않아야 함")
//    void searchExcludeDeleted() {
//        SellingBid bid = create(seller, productAirForce, SellingStatus.LIVE);
//        repository.save(bid);
//
//        // Soft Delete (Repository method or Direct ID setting if Setter available)
//        // 여기선 Repository delete 사용
//        repository.delete(bid);
//        em.flush();
//        em.clear();
//
//        Page<SellingBid> result = repository.findAdminSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));
//        assertThat(result.getContent()).isEmpty();
//    }
//
//    // 17. 페이징 처리
//    @Test
//    @DisplayName("페이징: 2번째 페이지 조회")
//    void testPaging() {
//        for (int i = 0; i < 5; i++) {
//            repository.save(create(seller, productAirForce, SellingStatus.LIVE));
//        }
//
//        // Page 0, Size 2
//        Page<SellingBid> page1 = repository.findAdminSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 2));
//        assertThat(page1.getContent()).hasSize(2);
//        assertThat(page1.getTotalElements()).isEqualTo(5);
//
//        // Page 1, Size 2
//        Page<SellingBid> page2 = repository.findAdminSellingBids(new SellingBidSearchCondition(), PageRequest.of(1, 2));
//        assertThat(page2.getContent()).hasSize(2);
//    }
//
//    // 18. 정렬 (최신순)
//    @Test
//    @DisplayName("정렬: 최신순(내림차순) 확인")
//    void testSorting() {
//        SellingBid oldBid = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(oldBid, LocalDate.now().minusDays(1).atStartOfDay());
//
//        SellingBid newBid = create(seller, productAirForce, SellingStatus.LIVE);
//        saveAndSetCreatedAt(newBid, LocalDate.now().atStartOfDay());
//
//        Page<SellingBid> result = repository.findAdminSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));
//
//        assertThat(result.getContent()).hasSize(2);
//        assertThat(result.getContent().get(0).getId()).isEqualTo(newBid.getId());
//    }
//}
