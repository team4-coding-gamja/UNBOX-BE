//package com.example.unbox_be.domain.user.repository;
//
//import com.example.unbox_be.domain.user.entity.User;
//import com.example.unbox_be.global.config.JpaAuditingConfig;
//import com.example.unbox_be.global.config.TestQueryDslConfig;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.data.domain.*;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
//@ActiveProfiles("test")
//@TestPropertySource(properties = {"spring.sql.init.mode=never"})
//class UserRepositoryTest {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @PersistenceContext
//    private EntityManager em;
//
//    // =========================
//    // ✅ 테스트 헬퍼
//    // =========================
//    private User 유저생성(int n) {
//        // nickname: User 엔티티 정규식이 "^[a-z0-9]{4,10}$" 라고 되어있어서 그에 맞춤
//        String email = "user" + n + "@test.com";
//        String pw = "pw1234!@#" + n; // password 검증이 강해져도 통과 가능하도록
//        String nickname = "user" + String.format("%03d", n); // user001 (7자) OK
//        String phone = "010-1111-" + String.format("%04d", n);
//
//        return User.createUser(email, pw, nickname, phone);
//    }
//
//    private void flushAndClear() {
//        em.flush();
//        em.clear();
//    }
//
//    // =========================
//    // ✅ 테스트
//    // =========================
//
//    @Nested
//    @DisplayName("existsByEmailAndDeletedAtIsNull / findByEmailAndDeletedAtIsNull")
//    class EmailQueries {
//
//        @Test
//        @DisplayName("삭제되지 않은 유저는 exists/find가 true/조회된다")
//        void exists_find_success_when_not_deleted() {
//            // given
//            User user = 유저생성(1);
//            em.persist(user);
//            flushAndClear();
//
//            // when
//            boolean exists = userRepository.existsByEmailAndDeletedAtIsNull("user1@test.com");
//            Optional<User> found = userRepository.findByEmailAndDeletedAtIsNull("user1@test.com");
//
//            // then
//            assertThat(exists).isTrue();
//            assertThat(found).isPresent();
//            assertThat(found.get().getEmail()).isEqualTo("user1@test.com");
//        }
//
//        @Test
//        @DisplayName("softDelete 된 유저는 exists/find가 false/empty다")
//        void exists_find_empty_when_soft_deleted() {
//            // given
//            User user = 유저생성(1);
//            em.persist(user);
//            flushAndClear();
//
//            // when
//            User managed = em.find(User.class, user.getId());
//            managed.softDelete("tester");
//            flushAndClear();
//
//            boolean exists = userRepository.existsByEmailAndDeletedAtIsNull("user1@test.com");
//            Optional<User> found = userRepository.findByEmailAndDeletedAtIsNull("user1@test.com");
//
//            // then
//            assertThat(exists).isFalse();
//            assertThat(found).isEmpty();
//        }
//    }
//
//    @Nested
//    @DisplayName("findByIdAndDeletedAtIsNull")
//    class FindById {
//
//        @Test
//        @DisplayName("삭제되지 않은 유저는 id로 조회된다")
//        void findById_success() {
//            // given
//            User user = 유저생성(1);
//            em.persist(user);
//            flushAndClear();
//
//            // when
//            Optional<User> found = userRepository.findByIdAndDeletedAtIsNull(user.getId());
//
//            // then
//            assertThat(found).isPresent();
//            assertThat(found.get().getNickname()).isEqualTo("user001");
//        }
//
//        @Test
//        @DisplayName("softDelete 된 유저는 id로 조회되지 않는다")
//        void findById_empty_when_soft_deleted() {
//            // given
//            User user = 유저생성(1);
//            em.persist(user);
//            flushAndClear();
//
//            // when
//            User managed = em.find(User.class, user.getId());
//            managed.softDelete("tester");
//            flushAndClear();
//
//            Optional<User> found = userRepository.findByIdAndDeletedAtIsNull(user.getId());
//
//            // then
//            assertThat(found).isEmpty();
//        }
//    }
//
//    @Test
//    @DisplayName("findAllByDeletedAtIsNull: 페이징 조회 시 softDelete 제외된다")
//    void findAllByDeletedAtIsNull_paging_excludes_deleted() {
//        // given
//        User u1 = 유저생성(1);
//        User u2 = 유저생성(2);
//        User u3 = 유저생성(3);
//
//        em.persist(u1);
//        em.persist(u2);
//        em.persist(u3);
//        flushAndClear();
//
//        // u2 soft delete
//        User managedU2 = em.find(User.class, u2.getId());
//        managedU2.softDelete("tester");
//        flushAndClear();
//
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
//
//        // when
//        Page<User> page = userRepository.findAllByDeletedAtIsNull(pageable);
//
//        // then
//        assertThat(page.getTotalElements()).isEqualTo(2);
//        assertThat(page.getContent())
//                .extracting(User::getEmail)
//                .containsExactlyInAnyOrder("user1@test.com", "user3@test.com");
//    }
//
//    @Test
//    @DisplayName("email UNIQUE: 같은 이메일 저장 시 예외")
//    void unique_email_throws() {
//        // given
//        User u1 = User.createUser("dup@test.com", "pw1234!@#", "user111", "010-1111-1111");
//        User u2 = User.createUser("dup@test.com", "pw1234!@#", "user222", "010-1111-2222");
//
//        em.persist(u1);
//
//        // then ✅ IDENTITY면 여기서 바로 insert가 나가며 예외가 터질 수 있음
//        assertThatThrownBy(() -> em.persist(u2))
//                .isInstanceOfAny(org.hibernate.exception.ConstraintViolationException.class,
//                        DataIntegrityViolationException.class,
//                        RuntimeException.class);
//    }
//
//
//    @Test
//    @DisplayName("nickname UNIQUE: 같은 닉네임 저장 시 예외")
//    void unique_nickname_throws() {
//        // given
//        User u1 = User.createUser("a@test.com", "pw1234!@#", "user777", "010-1111-7777");
//        User u2 = User.createUser("b@test.com", "pw1234!@#", "user777", "010-1111-8888");
//
//        em.persist(u1);
//
//        // then ✅ IDENTITY면 여기서 바로 insert가 나가며 예외가 터질 수 있음
//        assertThatThrownBy(() -> em.persist(u2))
//                .isInstanceOfAny(org.hibernate.exception.ConstraintViolationException.class,
//                        DataIntegrityViolationException.class,
//                        RuntimeException.class);
//    }
//}