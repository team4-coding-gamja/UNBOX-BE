package com.example.unbox_be.domain.product.repository;

import com.example.unbox_be.domain.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    //Optional<Brand> findByName(String name);
}
