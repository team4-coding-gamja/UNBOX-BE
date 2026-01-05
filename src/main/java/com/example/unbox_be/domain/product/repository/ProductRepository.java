package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.dto.ProductSearchCondition;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    // 목록 조회: brand를 같이 땡겨 N+1 줄이기
    @EntityGraph(attributePaths = {"brand"})
    @Query("""
        select p
        from Product p
        where (:brandId is null or p.brand.id = :brandId)
          and (:category is null or p.category = :category)
          and (
                :keyword is null
                or :keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.modelNumber) like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<Product> findByFilters(UUID brandId, Category category, String keyword, Pageable pageable);

    // 상세 조회: brand fetch
    @EntityGraph(attributePaths = {"brand"})
    Optional<Product> findById(UUID id);

    // 이름을 명확히 하고 싶으면 별도 메서드로
    @EntityGraph(attributePaths = {"brand"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdWithBrand(UUID id);

}
