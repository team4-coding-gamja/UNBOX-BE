//package com.example.unbox_be.domain.product.repository;
//
//import com.example.unbox_be.domain.product.entity.Brand;
//import com.example.unbox_be.global.config.JpaAuditingConfig;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import com.example.unbox_be.global.config.TestQueryDslConfig;
//import org.springframework.context.annotation.Import;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.test.context.TestPropertySource;
//
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
//@TestPropertySource(properties = {"spring.sql.init.mode=never"})
//class BrandRepositoryTest {
//
//    @Autowired
//    private BrandRepository brandRepository;
//
//    @PersistenceContext
//    private EntityManager em; // 영속성 컨텍스트 제어를 위해 필요
//
//    // 테스트용 데이터 생성 헬퍼
//    private Brand createAndSaveBrand(String name) {
//        // 엔티티의 validation 규칙(URL 형식 등)을 지켜야 생성됨
//        String validUrl = "https://example.com/logo.png";
//        Brand brand = Brand.createBrand(name, validUrl);
//        return brandRepository.save(brand);
//    }
//
//    @Test
//    @DisplayName("Soft Delete 확인")
//    void softDeleteCheck() {
//        // Given
//        Brand brand = createAndSaveBrand("Nike");
//        UUID brandId = brand.getId();
//
//        // When
//        brandRepository.delete(brand); // @SQLDelete 작동 -> Update 쿼리 실행됨
//
//        // 중요: 영속성 컨텍스트에 남은 캐시를 비워야 DB에서 다시 조회함
//        em.flush();
//        em.clear();
//
//        // Then
//        // 1. 기본 findById 조회 시 없어야 함 (@SQLRestriction 적용됨)
//        Optional<Brand> foundBrand = brandRepository.findById(brandId);
//        assertThat(foundBrand).isEmpty();
//
//        // 2. 명시적 메서드 findByIdAndDeletedAtIsNull 조회 시에도 없어야 함
//        Optional<Brand> foundBrandExplicit = brandRepository.findByIdAndDeletedAtIsNull(brandId);
//        assertThat(foundBrandExplicit).isEmpty();
//    }
//
//    @Test
//    @DisplayName("전체 조회: 삭제된 데이터는 목록에서 제외된다")
//    void findAllByDeletedAtIsNull() {
//        // Given
//        createAndSaveBrand("Nike"); // 생존
//
//        Brand deletedBrand = createAndSaveBrand("Adidas");
//        brandRepository.delete(deletedBrand); // 삭제 처리 (@SQLDelete)
//
//        em.flush();
//        em.clear();
//
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // When
//        Page<Brand> result = brandRepository.findAllByDeletedAtIsNull(pageable);
//
//        // Then
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).getName()).isEqualTo("Nike");
//    }
//
//    @Test
//    @DisplayName("중복 체크: 삭제된 브랜드 이름은 다시 사용 가능하다 (중복 아님)")
//    void existsByNameAndDeletedAtIsNull_Deleted() {
//        // Given
//        Brand brand = createAndSaveBrand("Nike");
//        brandRepository.delete(brand); // 삭제
//
//        em.flush();
//        em.clear();
//
//        // When
//        boolean exists = brandRepository.existsByNameAndDeletedAtIsNull("Nike");
//
//        // Then
//        assertThat(exists).isFalse(); // 삭제되었으므로 false
//    }
//
//    @Test
//    @DisplayName("수정 시 중복 체크: 내 이름은 중복 아님, 남의 이름은 중복")
//    void existsByNameAndIdNot_Test() {
//        // Given
//        Brand myBrand = createAndSaveBrand("Nike");
//        createAndSaveBrand("Adidas"); // 다른 브랜드
//
//        em.flush();
//        em.clear();
//
//        // When & Then
//        // 1. 내 이름("Nike") 그대로 유지 -> 중복 아님
//        boolean isDuplicateSelf = brandRepository.existsByNameAndIdNotAndDeletedAtIsNull("Nike", myBrand.getId());
//        assertThat(isDuplicateSelf).isFalse();
//
//        // 2. 남의 이름("Adidas")으로 변경 시도 -> 중복
//        boolean isDuplicateOther = brandRepository.existsByNameAndIdNotAndDeletedAtIsNull("Adidas", myBrand.getId());
//        assertThat(isDuplicateOther).isTrue();
//    }
//
//    @Test
//    @DisplayName("검색: 대소문자 구분 없이 검색되며 삭제된 건 제외된다")
//    void searchByName() {
//        // Given
//        createAndSaveBrand("Nike Air");
//
//        Brand deleted = createAndSaveBrand("Nike Pro");
//        brandRepository.delete(deleted); // 삭제
//
//        em.flush();
//        em.clear();
//
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // When (키워드: "niKE")
//        Page<Brand> result = brandRepository.searchByNameAndDeletedAtIsNull("niKE", pageable);
//
//        // Then
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).getName()).isEqualTo("Nike Air");
//    }
//}