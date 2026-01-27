package com.example.unbox_product.product.domain.repository;

import com.example.unbox_common.config.JpaConfig;
import com.example.unbox_product.product.domain.entity.Brand;
import com.example.unbox_product.product.domain.entity.Category;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.entity.ProductOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
public class ProductOptionRepositoryTest {

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    private Brand brand;
    private Product product;
    private ProductOption productOption1;
    private ProductOption productOption2;

    @BeforeEach
    void setUp() {
        brand = Brand.createBrand("Test Brand", "http://test.com/logo.png");
        brandRepository.save(brand);

        product = Product.createProduct("Test Product", "MODEL-001", Category.ELECTRONICS, "http://test.com/image.png",
                brand);
        productRepository.save(product);

        productOption1 = ProductOption.createProductOption(product, "Option 1");
        productOption2 = ProductOption.createProductOption(product, "Option 2");
        productOptionRepository.saveAll(List.of(productOption1, productOption2));
    }

    @Test
    @DisplayName("deleteByProductId should soft delete all options for a given product")
    void deleteByProductId_shouldSoftDeleteOptions() {
        // When
        productOptionRepository.deleteByProductId(product.getId(), "admin@test.com");

        // Then
        List<ProductOption> options = productOptionRepository.findAll();
        assertThat(options).hasSize(2);

        for (ProductOption option : options) {
            assertThat(option.getDeletedAt()).isNotNull();
            assertThat(option.getDeletedBy()).isEqualTo("admin@test.com");
        }

        Optional<ProductOption> deletedOption = productOptionRepository
                .findByIdAndDeletedAtIsNull(productOption1.getId());
        assertThat(deletedOption).isEmpty();
    }

    @Test
    @DisplayName("deleteByProductIdsIn should soft delete all options for given products")
    void deleteByProductIdsIn_shouldSoftDeleteOptions() {
        // When
        productOptionRepository.deleteByProductIdsIn(List.of(product.getId()), "admin@test.com");

        // Then
        List<ProductOption> options = productOptionRepository.findAll();
        assertThat(options).hasSize(2);

        for (ProductOption option : options) {
            assertThat(option.getDeletedAt()).isNotNull();
            assertThat(option.getDeletedBy()).isEqualTo("admin@test.com");
        }
    }
}
