package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Brand;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
//import java.util.Optional;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    // 중복 체크
    boolean existsByName (String name);

    // 브랜드 아이디로 조회
    Optional<Brand> findById(UUID id);

    @Query("""
    select b
    from Brand b
    where lower(b.name) like lower(concat('%', :keyword, '%'))
""")
    Page<Brand> searchByName(@Param("keyword") String keyword, Pageable pageable);

}
