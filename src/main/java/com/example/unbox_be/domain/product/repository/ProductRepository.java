package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // ✅ 목록 조회(삭제 포함) - brand 같이 fetch
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

    // ✅ 목록 조회(삭제 제외) - brand 같이 fetch (실무 권장)
    @EntityGraph(attributePaths = {"brand"})
    @Query("""
        select p
        from Product p
        where p.deletedAt is null
          and (:brandId is null or p.brand.id = :brandId)
          and (:category is null or p.category = :category)
          and (
                :keyword is null
                or :keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.modelNumber) like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<Product> findByFiltersAndDeletedAtIsNull(UUID brandId, Category category, String keyword, Pageable pageable);

    // ✅ 상세 조회(삭제 포함) - brand fetch
    @EntityGraph(attributePaths = {"brand"})
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    // ✅ 상세 조회(삭제 제외) - brand fetch (실무 권장)
    @EntityGraph(attributePaths = {"brand"})
    Optional<Product> findByIdAndDeletedAtIsNullAndDeletedAtIsNull(UUID id);

    // ✅ brand fetch 명확 버전(삭제 포함)
    @EntityGraph(attributePaths = {"brand"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdAndDeletedAtIsNullWithBrand(UUID id);

    // ✅ brand fetch 명확 버전(삭제 제외)
    @EntityGraph(attributePaths = {"brand"})
    @Query("select p from Product p where p.id = :id and p.deletedAt is null")
    Optional<Product> findByIdAndDeletedAtIsNullWithBrandAndDeletedAtIsNull(UUID id);

    // ✅ 모델번호 중복 체크(본인 제외) - 삭제 제외
    boolean existsByModelNumberAndIdNotAndDeletedAtIsNull(String modelNumber, UUID id);

    // ✅ 리뷰 수, 리뷰 총점 추가(리뷰 추가시)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.reviewCount = p.reviewCount + 1, p.totalScore = p.totalScore + :score WHERE p.id = :id")
    int increaseReviewCountAndScore(@Param("id") UUID id, @Param("score") int score);

    // ✅ 리뷰 수, 리뷰 총점 삭제(리뷰 삭제시)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.reviewCount = p.reviewCount - 1, p.totalScore = p.totalScore - :score WHERE p.id = :id")
    int decreaseReviewCountAndScore(@Param("id") UUID id, @Param("score") int score);

    // ✅ 리뷰 수, 리뷰 총점 업데이트(리뷰 수정시)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.totalScore = p.totalScore + :delta WHERE p.id = :id")
    int updateReviewCountAndScore(@Param("id") UUID id, @Param("delta") int delta);

}
