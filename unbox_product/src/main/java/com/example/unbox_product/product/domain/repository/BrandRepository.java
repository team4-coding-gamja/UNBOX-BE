package com.example.unbox_product.product.domain.repository;

import com.example.unbox_product.product.domain.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

  // 브랜드 목록 조회
  Page<Brand> findAllByDeletedAtIsNull(Pageable pageable);

  // 이름으로 중복 체크
  boolean existsByNameAndDeletedAtIsNull(String name);

  // 브랜드 아이디로 조회
  Optional<Brand> findByIdAndDeletedAtIsNull(UUID id);

  // 수정 시 중복 체크(본인 제외)
  boolean existsByNameAndIdNotAndDeletedAtIsNull(String name, UUID id);

  // 브랜드 이름 검색 - 삭제 제외
  @Query("""
      select b
      from Brand b
      where b.deletedAt is null
        and lower(b.name) like lower(concat('%', :keyword, '%'))
      """)
  Page<Brand> searchByNameAndDeletedAtIsNull(@Param("keyword") String keyword, Pageable pageable);

}
