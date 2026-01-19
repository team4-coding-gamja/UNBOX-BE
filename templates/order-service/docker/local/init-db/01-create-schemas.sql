-- Order Service Database Schema
-- 이 파일은 주문 관리 서비스의 데이터베이스 스키마 생성용입니다.

-- 테이블 생성 예시
-- 개발자가 실제 Order 엔티티에 맞게 수정해주세요

/*
-- 예시: 주문 테이블
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL, -- User Service의 사용자 ID 참조
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'
    total_amount DECIMAL(10,2) NOT NULL,
    shipping_fee DECIMAL(10,2) DEFAULT 0,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20), -- 'CARD', 'BANK_TRANSFER', 'CASH' 등
    shipping_address TEXT,
    shipping_phone VARCHAR(20),
    shipping_name VARCHAR(100),
    order_memo TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP
);

-- 예시: 주문 상품 테이블
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL, -- Product Service의 상품 ID 참조
    product_name VARCHAR(255) NOT NULL, -- 주문 시점의 상품명 저장
    product_price DECIMAL(10,2) NOT NULL, -- 주문 시점의 상품 가격 저장
    quantity INTEGER NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    product_options TEXT, -- JSON 형태로 옵션 정보 저장
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예시: 주문 상태 이력 테이블
CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by BIGINT, -- User Service의 사용자 ID 참조 (시스템 변경인 경우 NULL)
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예시: 배송 정보 테이블
CREATE TABLE IF NOT EXISTS shipping_info (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    courier_company VARCHAR(50),
    tracking_number VARCHAR(100),
    shipping_status VARCHAR(20) DEFAULT 'PREPARING', -- 'PREPARING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED'
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_shipping_info_order_id ON shipping_info(order_id);
*/

-- TODO: 실제 Order Service 엔티티에 맞는 테이블 생성 SQL을 작성해주세요