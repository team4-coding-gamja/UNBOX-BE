-- Trade Service Database Schema
-- 이 파일은 거래 관리 서비스의 데이터베이스 스키마 생성용입니다.

-- 테이블 생성 예시
-- 개발자가 실제 Trade 엔티티에 맞게 수정해주세요

/*
-- 예시: 거래 테이블
CREATE TABLE IF NOT EXISTS trades (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL, -- User Service의 사용자 ID 참조
    buyer_id BIGINT NOT NULL,  -- User Service의 사용자 ID 참조
    product_id BIGINT NOT NULL, -- Product Service의 상품 ID 참조
    trade_type VARCHAR(20) NOT NULL, -- 'SELL', 'BUY', 'EXCHANGE' 등
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'
    price DECIMAL(10,2) NOT NULL,
    quantity INTEGER DEFAULT 1,
    trade_location VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- 예시: 거래 메시지 테이블
CREATE TABLE IF NOT EXISTS trade_messages (
    id BIGSERIAL PRIMARY KEY,
    trade_id BIGINT NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL, -- User Service의 사용자 ID 참조
    message TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT', -- 'TEXT', 'IMAGE', 'SYSTEM'
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예시: 거래 리뷰 테이블
CREATE TABLE IF NOT EXISTS trade_reviews (
    id BIGSERIAL PRIMARY KEY,
    trade_id BIGINT NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL, -- User Service의 사용자 ID 참조
    reviewee_id BIGINT NOT NULL, -- User Service의 사용자 ID 참조
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예시: 거래 상태 이력 테이블
CREATE TABLE IF NOT EXISTS trade_status_history (
    id BIGSERIAL PRIMARY KEY,
    trade_id BIGINT NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by BIGINT NOT NULL, -- User Service의 사용자 ID 참조
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_trades_seller_id ON trades(seller_id);
CREATE INDEX IF NOT EXISTS idx_trades_buyer_id ON trades(buyer_id);
CREATE INDEX IF NOT EXISTS idx_trades_product_id ON trades(product_id);
CREATE INDEX IF NOT EXISTS idx_trades_status ON trades(status);
CREATE INDEX IF NOT EXISTS idx_trade_messages_trade_id ON trade_messages(trade_id);
CREATE INDEX IF NOT EXISTS idx_trade_reviews_trade_id ON trade_reviews(trade_id);
*/

-- TODO: 실제 Trade Service 엔티티에 맞는 테이블 생성 SQL을 작성해주세요