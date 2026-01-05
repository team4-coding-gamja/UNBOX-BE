package com.example.unbox_be.global.init;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!prod") // 운영(prod) 환경이 아닐 때만 동작
public class GlobalDataInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1. Master 관리자 계정 생성
        initMasterAdmin();

        // 2. 초기 브랜드 및 상품 데이터 생성 (브랜드가 없을 때만 실행)
        if (brandRepository.count() == 0) {
            initBrandsAndProducts();
        }
    }

    private void initMasterAdmin() {
        // 이메일 중복 체크 (이미 존재하면 Skip)
        if (adminRepository.existsByEmail("master@unbox.com")) {
            return;
        }

        // 팩토리 메서드 + 닉네임 정규식(소문자+숫자 4~10자) 준수
        Admin master = Admin.createAdmin(
                "master@unbox.com",
                passwordEncoder.encode("12341234!"), // 암호화 필수
                "master01",                          // 닉네임 규칙 준수
                "010-1234-5678",
                AdminRole.ROLE_MASTER
        );

        adminRepository.save(master);
        log.info("=========== [Seed Data] Master Admin Created ===========");
        log.info("ID: master@unbox.com / PW: 12341234!");
        log.info("========================================================");
    }

    private void initBrandsAndProducts() {
        // 1. 브랜드 생성
        Brand nike = Brand.createBrand(
                "Nike",
                "https://dummyimage.com/200x200/000/fff&text=Nike"
        );

        Brand adidas = Brand.createBrand(
                "Adidas",
                "https://dummyimage.com/200x200/000/fff&text=Adidas"
        );

        brandRepository.save(nike);
        brandRepository.save(adidas);

        // 2. 상품 생성 (Category.SHOES 사용)
        Product jordan1 = Product.createProduct(
                "Jordan 1 Retro High OG Chicago 2022",
                "DZ5485-612",
                Category.SHOES,
                "https://dummyimage.com/600x600/000/fff&text=Jordan1",
                nike
        );

        Product yeezySlide = Product.createProduct(
                "Adidas Yeezy Slide Bone 2022",
                "FZ5897",
                Category.SHOES,
                "https://dummyimage.com/600x600/000/fff&text=Yeezy",
                adidas
        );

        productRepository.save(jordan1);
        productRepository.save(yeezySlide);

        log.info("=========== [Seed Data] Brand & Product Created ===========");
    }
}