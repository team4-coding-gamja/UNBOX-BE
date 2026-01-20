//package com.example.unbox_be.global.config;
//
//import com.querydsl.jpa.impl.JPAQueryFactory;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Primary;
//
//@TestConfiguration
//public class TestQueryDslConfig {
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    @Bean
//    @Primary // 테스트 환경에서 우선 사용
//    public JPAQueryFactory jpaQueryFactory() {
//        return new JPAQueryFactory(entityManager);
//    }
//}
