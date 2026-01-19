-- Order Service Seed Data
-- 이 파일은 주문 관리 서비스의 초기 데이터 삽입용입니다.

-- 개발자가 필요한 초기 데이터를 여기에 추가해주세요

/*
-- 예시: 테스트 주문 데이터
INSERT INTO orders (order_number, user_id, status, total_amount, shipping_fee, discount_amount, final_amount, payment_method, shipping_address, shipping_phone, shipping_name) VALUES
('ORD-2024-001', 1, 'DELIVERED', 299000.00, 3000.00, 0.00, 302000.00, 'CARD', '서울시 강남구 테헤란로 123', '010-1234-5678', '홍길동'),
('ORD-2024-002', 2, 'SHIPPED', 1299000.00, 0.00, 50000.00, 1249000.00, 'BANK_TRANSFER', '서울시 서초구 서초대로 456', '010-9876-5432', '김영희'),
('ORD-2024-003', 3, 'PENDING', 150000.00, 2500.00, 10000.00, 142500.00, 'CARD', '부산시 해운대구 해운대로 789', '010-5555-1234', '이철수')
ON CONFLICT (order_number) DO NOTHING;

-- 예시: 주문 상품 데이터
INSERT INTO order_items (order_id, product_id, product_name, product_price, quantity, total_price, product_options) VALUES
(1, 1, '테스트 스마트폰', 299000.00, 1, 299000.00, '{"color": "black", "storage": "128GB"}'),
(2, 2, '테스트 노트북', 1299000.00, 1, 1299000.00, '{"color": "silver", "ram": "16GB"}'),
(3, 1, '테스트 스마트폰', 299000.00, 1, 299000.00, '{"color": "white", "storage": "256GB"}')
ON CONFLICT DO NOTHING;

-- 예시: 주문 상태 이력
INSERT INTO order_status_history (order_id, previous_status, new_status, changed_by, reason) VALUES
(1, NULL, 'PENDING', 1, '주문 생성'),
(1, 'PENDING', 'CONFIRMED', NULL, '결제 완료'),
(1, 'CONFIRMED', 'SHIPPED', NULL, '배송 시작'),
(1, 'SHIPPED', 'DELIVERED', NULL, '배송 완료'),
(2, NULL, 'PENDING', 2, '주문 생성'),
(2, 'PENDING', 'CONFIRMED', NULL, '결제 완료'),
(2, 'CONFIRMED', 'SHIPPED', NULL, '배송 시작'),
(3, NULL, 'PENDING', 3, '주문 생성')
ON CONFLICT DO NOTHING;

-- 예시: 배송 정보
INSERT INTO shipping_info (order_id, courier_company, tracking_number, shipping_status, estimated_delivery_date, actual_delivery_date) VALUES
(1, 'CJ대한통운', '1234567890123', 'DELIVERED', '2024-01-15', '2024-01-15'),
(2, '한진택배', '9876543210987', 'IN_TRANSIT', '2024-01-20', NULL)
ON CONFLICT DO NOTHING;
*/

-- TODO: 실제 필요한 초기 주문 데이터를 여기에 추가해주세요
-- 예: 테스트 주문, 샘플 배송 정보, 기본 설정 등