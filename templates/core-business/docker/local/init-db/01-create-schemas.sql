-- Core Business Service Database Schema
-- 이 파일은 데이터베이스 스키마 생성용입니다.

-- 데이터베이스가 존재하지 않으면 생성 (Docker에서 자동 처리되므로 주석 처리)
-- CREATE DATABASE IF NOT EXISTS unbox_core_local;

-- 스키마 생성 예시 (필요에 따라 수정)
-- CREATE SCHEMA IF NOT EXISTS core_business;

-- 테이블 생성 예시
-- 개발자가 실제 엔티티에 맞게 수정해주세요

/*
-- 예시: 공통 설정 테이블
CREATE TABLE IF NOT EXISTS system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예시: API 게이트웨이 라우팅 설정
CREATE TABLE IF NOT EXISTS api_routes (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(50) NOT NULL,
    route_path VARCHAR(255) NOT NULL,
    target_url VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
*/

-- TODO: 실제 Core Business 엔티티에 맞는 테이블 생성 SQL을 작성해주세요