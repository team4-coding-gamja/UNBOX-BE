package com.example.unbox_be.domain.admin.common.repository;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.global.config.JpaAuditingConfig;
import com.example.unbox_be.global.config.TestQueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminRepositoryTest {

    @Autowired private AdminRepository adminRepository;
    @Autowired private EntityManager em;

    // =========================================================
    // ✅ 테스트 헬퍼
    // =========================================================
    private Admin 관리자생성(String email, String nickname, AdminRole role) {
        return Admin.createAdmin(email, "encoded-password", nickname, "010-0000-0000", role);
    }

    private void 삭제시간강제세팅(Object entity, LocalDateTime deletedAt) {
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("deletedAt");
                field.setAccessible(true);
                field.set(entity, deletedAt);
                return;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("deletedAt 필드 세팅 실패", e);
            }
        }
        throw new IllegalStateException("deletedAt 필드를 찾지 못했습니다. (BaseEntity 필드명 확인 필요)");
    }

    private void 플러시후영속성컨텍스트비우기() {
        em.flush();
        em.clear();
    }

    // =========================================================
    // ✅ existsByNickname
    // =========================================================
    @Nested
    @DisplayName("existsByNickname() - 닉네임 중복 확인")
    class 닉네임중복확인 {

        @Test
        @DisplayName("닉네임이 존재하면 true를 반환한다")
        void 닉네임이_존재하면_true를_반환한다() {
            Admin admin = 관리자생성("a1@test.com", "abcd1", AdminRole.ROLE_MASTER);
            adminRepository.save(admin);
            플러시후영속성컨텍스트비우기();

            boolean result = adminRepository.existsByNickname("abcd1");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("닉네임이 존재하지 않으면 false를 반환한다")
        void 닉네임이_존재하지_않으면_false를_반환한다() {
            boolean result = adminRepository.existsByNickname("nope1");
            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // ✅ existsByAdminRole
    // =========================================================
    @Nested
    @DisplayName("existsByAdminRole() - 특정 역할 존재 확인")
    class 특정역할존재확인 {

        @Test
        @DisplayName("ROLE_MASTER 관리자가 존재하면 true를 반환한다")
        void ROLE_MASTER가_존재하면_true를_반환한다() {
            adminRepository.save(관리자생성("m1@test.com", "abcd2", AdminRole.ROLE_MASTER));
            플러시후영속성컨텍스트비우기();

            boolean result = adminRepository.existsByAdminRole(AdminRole.ROLE_MASTER);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("ROLE_MASTER 관리자가 없으면 false를 반환한다")
        void ROLE_MASTER가_없으면_false를_반환한다() {
            adminRepository.save(관리자생성("g1@test.com", "abcd3", AdminRole.ROLE_MANAGER));
            플러시후영속성컨텍스트비우기();

            boolean result = adminRepository.existsByAdminRole(AdminRole.ROLE_MASTER);

            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // ✅ findByEmailAndDeletedAtIsNull / existsByEmailAndDeletedAtIsNull
    // =========================================================
    @Nested
    @DisplayName("이메일 기반 조회/존재 검증 - Soft Delete 제외")
    class 이메일기반조회 {

        @Test
        @DisplayName("삭제되지 않은 이메일이면 findByEmailAndDeletedAtIsNull로 조회된다")
        void 이메일조회_삭제되지않은관리자면_조회된다() {
            adminRepository.save(관리자생성("live@test.com", "abcd4", AdminRole.ROLE_MASTER));
            플러시후영속성컨텍스트비우기();

            Optional<Admin> result = adminRepository.findByEmailAndDeletedAtIsNull("live@test.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("live@test.com");
        }

        @Test
        @DisplayName("삭제된 이메일이면 findByEmailAndDeletedAtIsNull로 조회되지 않는다")
        void 이메일조회_삭제된관리자면_조회되지않는다() {
            Admin admin = adminRepository.save(관리자생성("deleted@test.com", "abcd5", AdminRole.ROLE_MASTER));

            삭제시간강제세팅(admin, LocalDateTime.now());
            플러시후영속성컨텍스트비우기();

            Optional<Admin> result = adminRepository.findByEmailAndDeletedAtIsNull("deleted@test.com");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("삭제되지 않은 이메일이면 existsByEmailAndDeletedAtIsNull는 true")
        void 이메일존재확인_삭제되지않으면_true() {
            adminRepository.save(관리자생성("exist@test.com", "abcd6", AdminRole.ROLE_MANAGER));
            플러시후영속성컨텍스트비우기();

            boolean result = adminRepository.existsByEmailAndDeletedAtIsNull("exist@test.com");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("삭제된 이메일이면 existsByEmailAndDeletedAtIsNull는 false")
        void 이메일존재확인_삭제되면_false() {
            Admin admin = adminRepository.save(관리자생성("gone@test.com", "abcd7", AdminRole.ROLE_MANAGER));

            삭제시간강제세팅(admin, LocalDateTime.now());
            플러시후영속성컨텍스트비우기();

            boolean result = adminRepository.existsByEmailAndDeletedAtIsNull("gone@test.com");

            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // ✅ findByIdAndDeletedAtIsNull
    // =========================================================
    @Nested
    @DisplayName("findByIdAndDeletedAtIsNull() - ID 조회 Soft Delete 제외")
    class 아이디조회 {

        @Test
        @DisplayName("삭제되지 않은 관리자는 ID로 조회된다")
        void 아이디조회_삭제되지않은관리자면_조회된다() {
            Admin saved = adminRepository.save(관리자생성("id1@test.com", "abcd8", AdminRole.ROLE_MASTER));
            플러시후영속성컨텍스트비우기();

            Optional<Admin> result = adminRepository.findByIdAndDeletedAtIsNull(saved.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("삭제된 관리자는 ID로 조회되지 않는다")
        void 아이디조회_삭제된관리자면_조회되지않는다() {
            Admin saved = adminRepository.save(관리자생성("id2@test.com", "abcd9", AdminRole.ROLE_MANAGER));

            삭제시간강제세팅(saved, LocalDateTime.now());
            플러시후영속성컨텍스트비우기();

            Optional<Admin> result = adminRepository.findByIdAndDeletedAtIsNull(saved.getId());

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // ✅ findByAdminRoleInAndDeletedAtIsNull / findAllByAdminRoleInAndDeletedAtIsNull
    // =========================================================
    @Nested
    @DisplayName("역할 목록 조회(Page) - Soft Delete 제외")
    class 역할목록조회 {

        @Test
        @DisplayName("findByAdminRoleInAndDeletedAtIsNull: ROLE_MASTER/ROLE_MANAGER만 조회된다")
        void 역할목록조회_findByAdminRoleInAndDeletedAtIsNull_정상조회() {
            adminRepository.save(관리자생성("r1@test.com", "abca1", AdminRole.ROLE_MASTER));
            adminRepository.save(관리자생성("r2@test.com", "abca2", AdminRole.ROLE_MANAGER));

            Admin deletedMaster = adminRepository.save(관리자생성("r3@test.com", "abca3", AdminRole.ROLE_MASTER));
            삭제시간강제세팅(deletedMaster, LocalDateTime.now());

            플러시후영속성컨텍스트비우기();

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));

            Page<Admin> result = adminRepository.findByAdminRoleInAndDeletedAtIsNull(
                    List.of(AdminRole.ROLE_MASTER, AdminRole.ROLE_MANAGER),
                    pageable
            );

            assertThat(result.getContent())
                    .extracting(Admin::getEmail)
                    .contains("r1@test.com", "r2@test.com")
                    .doesNotContain("r3@test.com");
        }

        @Test
        @DisplayName("findAllByAdminRoleInAndDeletedAtIsNull: ROLE_MASTER만 조회된다")
        void 역할목록조회_findAllByAdminRoleInAndDeletedAtIsNull_정상조회() {
            Admin master1 = adminRepository.save(관리자생성("a@test.com", "abcb1", AdminRole.ROLE_MASTER));
            adminRepository.save(관리자생성("b@test.com", "abcb2", AdminRole.ROLE_MANAGER));

            Admin deletedMaster = adminRepository.save(관리자생성("c@test.com", "abcb3", AdminRole.ROLE_MASTER));
            삭제시간강제세팅(deletedMaster, LocalDateTime.now());

            플러시후영속성컨텍스트비우기();

            Pageable pageable = PageRequest.of(0, 10);

            Page<Admin> result = adminRepository.findAllByAdminRoleInAndDeletedAtIsNull(
                    List.of(AdminRole.ROLE_MASTER),
                    pageable
            );

            assertThat(result.getContent())
                    .extracting(Admin::getEmail)
                    .contains(master1.getEmail())
                    .doesNotContain("b@test.com", "c@test.com");
        }
    }
}