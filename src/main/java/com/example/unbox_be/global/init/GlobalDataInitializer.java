package com.example.unbox_be.global.init;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initAdminIfNotExists(
                "master@unbox.com",
                "master1",
                "010-1234-5678",
                AdminRole.ROLE_MASTER
        );

        initAdminIfNotExists(
                "manager@unbox.com",
                "manager1",
                "010-1111-2222",
                AdminRole.ROLE_MANAGER
        );

        initAdminIfNotExists(
                "inspector@unbox.com",
                "inspector1",
                "010-3333-4444",
                AdminRole.ROLE_INSPECTOR
        );

        initUserIfNotExists(
                "user@unbox.com",
                "user1",
                "010-9999-8888"
        );

    }

    private void initAdminIfNotExists(
            String email,
            String nickname,
            String phone,
            AdminRole role
    ) {
        if (adminRepository.existsByEmail(email)) {
            return;
        }

        Admin admin = Admin.createAdmin(
                email,
                passwordEncoder.encode("12341234!"), // 공통 초기 비밀번호
                nickname,
                phone,
                role
        );

        adminRepository.save(admin);

        log.info("=========== [Seed Data] Admin Created ===========");
        log.info("ROLE: {}", role);
        log.info("ID: {} / PW: 12341234!", email);
        log.info("================================================");
    }

    private void initUserIfNotExists(
            String email,
            String nickname,
            String phone
    ) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = User.createUser(
                email,
                passwordEncoder.encode("12341234!"),
                nickname,
                phone
        );

        userRepository.save(user);

        log.info("=========== [Seed Data] User Created ===========");
        log.info("ID: {} / PW: 12341234!", email);
        log.info("================================================");
    }
}
