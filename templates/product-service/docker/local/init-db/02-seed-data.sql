-- Product Service Seed Data
-- 이 파일은 상품 관리 서비스의 초기 데이터 삽입용입니다.

-- 개발자가 필요한 초기 데이터를 여기에 추가해주세요

/*
-- 예시: 기본 카테고리 데이터
INSERT INTO categories (name, description, sort_order) VALUES
('전자제품', '전자제품 카테고리', 1),
('의류', '의류 카테고리', 2),
('도서', '도서 카테고리', 3),
('생활용품', '생활용품 카테고리', 4),
('스포츠', '스포츠용품 카테고리', 5)
ON CONFLICT DO NOTHING;

-- 예시: 하위 카테고리
INSERT INTO categories (name, description, parent_id, sort_order) VALUES
('스마트폰', '스마트폰 및 액세서리', (SELECT id FROM categories WHERE name = '전자제품'), 1),
('노트북', '노트북 및 컴퓨터', (SELECT id FROM categories WHERE name = '전자제품'), 2),
('남성의류', '남성 의류', (SELECT id FROM categories WHERE name = '의류'), 1),
('여성의류', '여성 의류', (SELECT id FROM categories WHERE name = '의류'), 2)
ON CONFLICT DO NOTHING;

-- 예시: 테스트 상품 데이터
INSERT INTO products (name, description, category_id, price, stock_quantity, sku, seller_id) VALUES
('테스트 상품 1', '테스트용 상품입니다.', 
 (SELECT id FROM categories WHERE name = '스마트폰'), 
 299000.00, 10, 'TEST-PHONE-001', 1),
('테스트 상품 2', '또 다른 테스트용 상품입니다.', 
 (SELECT id FROM categories WHERE name = '노트북'), 
 1299000.00, 5, 'TEST-LAPTOP-001', 1)
ON CONFLICT (sku) DO NOTHING;
*/

-- TODO: 실제 필요한 초기 상품 데이터를 여기에 추가해주세요
-- 예: 기본 카테고리, 테스트 상품, 샘플 데이터 등