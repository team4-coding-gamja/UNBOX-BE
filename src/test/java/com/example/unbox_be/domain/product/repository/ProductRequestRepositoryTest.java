//package com.example.unbox_be.domain.product.repository;
//
//import com.example.unbox_be.domain.product.entity.ProductRequest;
//import com.example.unbox_be.global.config.JpaAuditingConfig;
//import com.example.unbox_be.global.config.TestQueryDslConfig;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.TestPropertySource;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
//@TestPropertySource(properties = {"spring.sql.init.mode=never"})
//@DataJpaTest
//class ProductRequestRepositoryTest {
//
//    @Autowired
//    private ProductRequestRepository productRequestRepository;
//
//    @Autowired
//    private TestEntityManager em;
//
//    @Test
//    @DisplayName("ID로 조회 시 deletedAt이 null인(삭제되지 않은) 요청만 조회된다")
//    void findByIdAndDeletedAtIsNull() {
//        // given
//        // 1. 정상 데이터 생성
//        ProductRequest activeRequest = ProductRequest.createProductRequest(1L,"New Nike Shoes", "Nike");
//        em.persist(activeRequest);
//
//        // 2. 삭제된 데이터 생성
//        ProductRequest deletedRequest = ProductRequest.createProductRequest(2L, "Old Adidas Shoes", "Adidas");
//
//        // 삭제 처리 (엔티티 내부에 softDelete() 메서드가 있다고 가정)
//        deletedRequest.softDelete("test-user");
//
//        em.persist(deletedRequest);
//
//        // 영속성 컨텍스트 초기화 (DB 반영 후 조회 테스트)
//        em.flush();
//        em.clear();
//
//        // when
//        Optional<ProductRequest> foundActive = productRequestRepository.findByIdAndDeletedAtIsNull(activeRequest.getId());
//        Optional<ProductRequest> foundDeleted = productRequestRepository.findByIdAndDeletedAtIsNull(deletedRequest.getId());
//
//        // then
//        // 1. 정상 데이터는 존재해야 함
//        assertThat(foundActive).isPresent();
//        assertThat(foundActive.get().getName()).isEqualTo("New Nike Shoes");
//
//        // 2. 삭제된 데이터는 조회되지 않아야 함 (Optional.empty)
//        assertThat(foundDeleted).isEmpty();
//    }
//
//    @Test
//    @DisplayName("저장 동작 확인")
//    void saveProductRequest() {
//        // given
//        ProductRequest request = ProductRequest.createProductRequest(1L, "Test Product", "Test Brand");
//
//        // when
//        ProductRequest saved = productRequestRepository.save(request);
//
//        // then
//        assertThat(saved.getId()).isNotNull();
//        assertThat(saved.getName()).isEqualTo("Test Product");
//    }
//}