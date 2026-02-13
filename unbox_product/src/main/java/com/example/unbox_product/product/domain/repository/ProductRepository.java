package com.example.unbox_product.product.domain.repository;

import com.example.unbox_product.product.domain.entity.Category;
import com.example.unbox_product.product.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  // 목록 조회(삭제 포함) - brand 같이 fetch
  @EntityGraph(attributePaths = { "brand" })
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

  // 목록 조회(삭제 제외) - brand 같이 fetch (실무 권장)
  @EntityGraph(attributePaths = { "brand" })
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

  // 상세 조회(삭제 포함) - brand fetch
  @EntityGraph(attributePaths = { "brand" })
  Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

  // 상세 조회(삭제 제외) - brand fetch (실무 권장)
  @EntityGraph(attributePaths = { "brand" })
  Optional<Product> findByIdAndDeletedAtIsNullAndDeletedAtIsNull(UUID id);

  // brand fetch 명확 버전(삭제 포함)
  @EntityGraph(attributePaths = { "brand" })
  @Query("select p from Product p where p.id = :id")
  Optional<Product> findByIdAndDeletedAtIsNullWithBrand(UUID id);

  // brand fetch 명확 버전(삭제 제외)
  @EntityGraph(attributePaths = { "brand" })
  @Query("select p from Product p where p.id = :id and p.deletedAt is null")
  Optional<Product> findByIdAndDeletedAtIsNullWithBrandAndDeletedAtIsNull(UUID id);

  // 모델번호 중복 체크(본인 제외) - 삭제 제외
  boolean existsByModelNumberAndIdNotAndDeletedAtIsNull(String modelNumber, UUID id);

  // 브랜드 ID로 삭제되지 않은 상품 '리스트' 조회 (페이징 아님)
  List<Product> findAllByBrandIdAndDeletedAtIsNull(UUID brandId);

  boolean existsByIdAndDeletedAtIsNull(UUID id);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP, p.deletedBy = :deletedBy WHERE p.brand.id = :brandId AND p.deletedAt IS NULL")
  void deleteByBrandId(@Param("brandId") UUID brandId, @Param("deletedBy") String deletedBy);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP, p.deletedBy = :deletedBy WHERE p.id = :productId AND p.deletedAt IS NULL")
  void softDeleteById(@Param("productId") UUID productId, @Param("deletedBy") String deletedBy);

  @EntityGraph(attributePaths = { "brand" })
  @Query("""
      SELECT p FROM Product p
      WHERE p.deletedAt IS NULL
        AND (:lastProductId IS NULL OR p.id < :lastProductId)
        AND (:brandId IS NULL OR p.brand.id = :brandId)
        AND (:category IS NULL OR p.category = :category)
        AND (:keyword IS NULL OR :keyword = ''
             OR lower(p.name) LIKE lower(concat('%', :keyword, '%'))
             OR lower(p.modelNumber) LIKE lower(concat('%', :keyword, '%')))
      ORDER BY p.id DESC
      """)
  Slice<Product> findByNoOffset(
      @Param("lastProductId") UUID lastProductId,
      @Param("brandId") UUID brandId,
      @Param("category") Category category,
      @Param("keyword") String keyword,
      Pageable pageable);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Product p SET p.popularityScore = p.popularityScore + 1 WHERE p.id = :productId")
  void incrementPopularityScore(@Param("productId") UUID productId);

  @EntityGraph(attributePaths = { "brand" })
  @Query("""
      SELECT p FROM Product p
      WHERE p.deletedAt IS NULL
        AND (:brandId IS NULL OR p.brand.id = :brandId)
        AND (
            :lastScore IS NULL OR
            (p.popularityScore < :lastScore) OR
            (p.popularityScore = :lastScore AND p.id < :lastId)
        )
      ORDER BY p.popularityScore DESC, p.id DESC
      """)
  Slice<Product> findByPopularityNoOffset(
      @Param("lastScore") Long lastScore,
      @Param("lastId") UUID lastId,
      @Param("brandId") UUID brandId,
      Pageable pageable);
}
