-- Payment Service Database Schema
-- 이 파일은 결제 처리 서비스의 데이터베이스 스키마 생성용입니다.

-- 테이블 생성 예시
-- 개발자가 실제 Payment 엔티티에 맞게 수정해주세요

/*
-- 예시: 결제 테이블
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    payment_key VARCHAR(100) NOT NULL UNIQUE, -- PG사에서 제공하는 결제 키
    order_id BIGINT NOT NULL, -- Order Service의 주문 ID 참조
    user_id BIGINT NOT NULL, -- User Service의 사용자 ID 참조
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL, -- 'CARD', 'BANK_TRANSFER', 'VIRTUAL_ACCOUNT', 'MOBILE' 등
    payment_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED'
    pg_provider VARCHAR(20), -- 'TOSS', 'KAKAO', 'NAVER', 'NICE' 등
    pg_transaction_id VARCHAR(100), -- PG사 거래 ID
    approved_at TIMESTAMP,
    failed_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예시: 결제 상세 정보 테이블
CREATE TABLE IF NOT EXISTS payment_details (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    card_company VARCHAR(50), -- 카드사명
    card_number VARCHAR(20), -- 마스킹된 카드번호
    installment_months INTEGER DEFAULT 0, -- 할부 개월수
    approval_number VARCHAR(20), -- 승인번호
    receipt_url VARCHAR(500), -- 영수증 URL
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예시: 환불 테이블
CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    refund_key VARCHAR(100) NOT NULL UNIQUE, -- PG사에서 제공하는 환불 키
    refund_amount DECIMAL(10,2) NOT NULL,
    refund_reason TEXT NOT NULL,
    refund_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'COMPLETED', 'FAILED'
    pg_refund_id VARCHAR(100), -- PG사 환불 ID
    requested_by BIGINT NOT NULL, -- User Service의 사용자 ID 참조
    approved_by BIGINT, -- User Service의 관리자 ID 참조
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예시: 결제 이벤트 로그 테이블 (감사 목적)
CREATE TABLE IF NOT EXISTS payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL, -- 'PAYMENT_REQUESTED', 'PAYMENT_APPROVED', 'PAYMENT_FAILED', 'REFUND_REQUESTED' 등
    event_data JSONB, -- 이벤트 관련 상세 데이터
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT -- User Service의 사용자 ID 참조 (시스템인 경우 NULL)
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_payments_payment_key ON payments(payment_key);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_pg_transaction_id ON payments(pg_transaction_id);
CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_payment_id ON payment_events(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_event_type ON payment_events(event_type);
*/

-- TODO: 실제 Payment Service 엔티티에 맞는 테이블 생성 SQL을 작성해주세요