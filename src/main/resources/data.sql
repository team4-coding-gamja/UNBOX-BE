-- =========================
-- Brands (나이키, 아디다스) : UUID 직접 지정
-- =========================
INSERT INTO p_brands (
    brand_id, created_at, updated_at, deleted_at,
    name, logo_url, created_by, updated_by, deleted_by
) VALUES
      ('11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'NIKE', 'https://logo.example/NIKE.png', 'system', 'system', NULL),

      ('22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'ADIDAS', 'https://logo.example/ADIDAS.png', 'system', 'system', NULL);


-- =========================
-- Products (각 브랜드 3개씩) : product_id UUID 직접 지정, category는 SHOES 고정
-- =========================
INSERT INTO p_products (
    product_id, brand_id,
    created_at, updated_at, deleted_at,
    name, model_number, image_url,
    created_by, updated_by, deleted_by,
    category
) VALUES    
      -- NIKE 3개
      ('aaaaaaaa-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'Nike Dunk Low', 'NK-DUNK-LOW-01', 'https://img.example/nike_dunk_low.png',
       'system', 'system', NULL, 'SHOES'),

      ('aaaaaaaa-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'Nike Air Force 1', 'NK-AF1-01', 'https://img.example/nike_af1.png',
       'system', 'system', NULL, 'SHOES'),

      ('aaaaaaaa-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'Nike Air Max 97', 'NK-AM97-01', 'https://img.example/nike_am97.png',
       'system', 'system', NULL, 'SHOES'),

      -- ADIDAS 3개
      ('bbbbbbbb-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'Adidas Samba', 'AD-SAMBA-01', 'https://img.example/adidas_samba.png',
       'system', 'system', NULL, 'SHOES'),

      ('bbbbbbbb-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'Adidas Gazelle', 'AD-GAZELLE-01', 'https://img.example/adidas_gazelle.png',
       'system', 'system', NULL, 'SHOES'),

      ('bbbbbbbb-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'Adidas Superstar', 'AD-SUPERSTAR-01', 'https://img.example/adidas_superstar.png',
       'system', 'system', NULL, 'SHOES');


-- =========================
-- Product Options (각 상품 5개: 신발 사이즈)
-- option_id UUID 직접 지정
-- =========================
INSERT INTO p_product_options (
    option_id, product_id,
    created_at, updated_at, deleted_at,
    option, created_by, updated_by, deleted_by
) VALUES
      -- Nike Dunk Low
      ('aaaa0000-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '240', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '245', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '250', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '255', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-000000000005', 'aaaaaaaa-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '260', 'system', 'system', NULL),

      -- Nike Air Force 1
      ('aaaa0000-0000-0000-0000-000000000006', 'aaaaaaaa-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '240', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-000000000007', 'aaaaaaaa-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '245', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-000000000008', 'aaaaaaaa-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '250', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-000000000009', 'aaaaaaaa-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '255', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-00000000000a', 'aaaaaaaa-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '260', 'system', 'system', NULL),

      -- Nike Air Max 97
      ('aaaa0000-0000-0000-0000-00000000000b', 'aaaaaaaa-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '240', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-00000000000c', 'aaaaaaaa-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '245', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-00000000000d', 'aaaaaaaa-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '250', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-00000000000e', 'aaaaaaaa-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '255', 'system', 'system', NULL),
      ('aaaa0000-0000-0000-0000-00000000000f', 'aaaaaaaa-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '260', 'system', 'system', NULL),

      -- Adidas Samba
      ('bbbb0000-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '240', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '245', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-000000000003', 'bbbbbbbb-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '250', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-000000000004', 'bbbbbbbb-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '255', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-000000000005', 'bbbbbbbb-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '260', 'system', 'system', NULL),

      -- Adidas Gazelle
      ('bbbb0000-0000-0000-0000-000000000006', 'bbbbbbbb-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '240', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-000000000007', 'bbbbbbbb-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '245', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-000000000008', 'bbbbbbbb-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '250', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-000000000009', 'bbbbbbbb-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '255', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-00000000000a', 'bbbbbbbb-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '260', 'system', 'system', NULL),

      -- Adidas Superstar
      ('bbbb0000-0000-0000-0000-00000000000b', 'bbbbbbbb-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '240', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-00000000000c', 'bbbbbbbb-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '245', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-00000000000d', 'bbbbbbbb-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '250', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-00000000000e', 'bbbbbbbb-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '255', 'system', 'system', NULL),
      ('bbbb0000-0000-0000-0000-00000000000f', 'bbbbbbbb-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, '260', 'system', 'system', NULL);

-- =========================
-- Users (구매자, 판매자) - GlobalDataInitializer와 생성을 맞추기 위해 미리 정의 (FK 제약조건 때문)
-- Table: p_users (User.java @Table 확인됨)
-- IDs: 1~7 (Auto-increment 가정하되 명시적 삽입)
-- =========================
INSERT INTO p_users (
    id, created_at, updated_at, deleted_at,
    email, password, nickname, phone, created_by, updated_by, deleted_by
) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'user@unbox.com', '{noop}12341234!', 'user1', '010-9999-8888', 'system', 'system', NULL),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'buyer1@unbox.com', '{noop}12341234!', 'buyer1', '010-1000-0001', 'system', 'system', NULL),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'buyer2@unbox.com', '{noop}12341234!', 'buyer2', '010-1000-0002', 'system', 'system', NULL),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'buyer3@unbox.com', '{noop}12341234!', 'buyer3', '010-1000-0003', 'system', 'system', NULL),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'seller1@unbox.com', '{noop}12341234!', 'seller1', '010-2000-0001', 'system', 'system', NULL),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'seller2@unbox.com', '{noop}12341234!', 'seller2', '010-2000-0002', 'system', 'system', NULL),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'seller3@unbox.com', '{noop}12341234!', 'seller3', '010-2000-0003', 'system', 'system', NULL);


-- =========================
-- Orders (주문 데이터 - 실제 서비스 상태 흐름 반영)
-- user_id: 위에서 생성한 Long ID (2: buyer1, 3: buyer2, 4: buyer3)
-- =========================

-- 1. PENDING_SHIPMENT (발송 대기) - Buyer1 (ID: 2) -> Seller1 (ID: 5)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price, 
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 
 2, 5, 'eeeeeeee-0000-0000-0000-000000000001', 'aaaa0000-0000-0000-0000-000000000003', 'PENDING_SHIPMENT', 150000, 
 '구매자1', '010-1000-0001', '서울시 강남구 테헤란로 123', '06234',
 'system', 'system', NULL);

-- 2. SHIPPED_TO_CENTER (센터로 발송됨 - 운송장 있음) - Buyer2 (ID: 3) -> Seller2 (ID: 6)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price, 
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    tracking_number, 
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP, NULL, 
 3, 6, 'eeeeeeee-0000-0000-0000-000000000002', 'aaaa0000-0000-0000-0000-000000000006', 'SHIPPED_TO_CENTER', 130000, 
 '구매자2', '010-1000-0002', '경기도 성남시 분당구 판교역로 456', '13522',
 'S-TRACK-001', 
 'system', 'system', NULL);

-- 3. IN_INSPECTION (검수 중 - 센터 도착 완료) - Buyer3 (ID: 4) -> Seller3 (ID: 7)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price, 
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    tracking_number, 
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000003', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP, NULL, 
 4, 7, 'eeeeeeee-0000-0000-0000-000000000003', 'bbbb0000-0000-0000-0000-000000000001', 'IN_INSPECTION', 120000, 
 '구매자3', '010-1000-0003', '서울시 서초구 서초대로 777', '06611',
 'S-TRACK-002', 
 'system', 'system', NULL);

-- 4. INSPECTION_PASSED (검수 합격 - 사용자 발송 준비) - Buyer1 (ID: 2) -> Seller1 (ID: 5)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price, 
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    tracking_number, 
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000004', CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP, NULL, 
 2, 5, 'eeeeeeee-0000-0000-0000-000000000004', 'bbbb0000-0000-0000-0000-000000000006', 'INSPECTION_PASSED', 125000, 
 '구매자1', '010-1000-0001', '부산시 해운대구', '48099',
 'S-TRACK-003', 
 'system', 'system', NULL);

-- 5. SHIPPED_TO_BUYER (구매자에게 배송 중) - Buyer2 (ID: 3) -> Seller2 (ID: 6)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price, 
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    tracking_number, final_tracking_number,
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000005', CURRENT_TIMESTAMP - INTERVAL '4' DAY, CURRENT_TIMESTAMP, NULL, 
 3, 6, 'eeeeeeee-0000-0000-0000-000000000005', 'aaaa0000-0000-0000-0000-00000000000e', 'SHIPPED_TO_BUYER', 180000, 
 '구매자2', '010-1000-0002', '대구시 수성구', '42011',
 'S-TRACK-004', 'B-TRACK-001', 
 'system', 'system', NULL);

-- 6. DELIVERED (배송 완료) - Buyer3 (ID: 4) -> Seller3 (ID: 7)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price, 
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    tracking_number, final_tracking_number,
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000006', CURRENT_TIMESTAMP - INTERVAL '5' DAY, CURRENT_TIMESTAMP, NULL, 
 4, 7, 'eeeeeeee-0000-0000-0000-000000000006', 'bbbb0000-0000-0000-0000-00000000000b', 'DELIVERED', 110000, 
 '구매자3', '010-1000-0003', '광주시 서구', '61922',
 'S-TRACK-005', 'B-TRACK-002', 
 'system', 'system', NULL);

-- 7. COMPLETED (거래 완료 - 구매 확정) - Buyer1 (ID: 2) -> Seller1 (ID: 5)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price, 
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    tracking_number, final_tracking_number, completed_at,
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000007', CURRENT_TIMESTAMP - INTERVAL '10' DAY, CURRENT_TIMESTAMP, NULL, 
 2, 5, 'eeeeeeee-0000-0000-0000-000000000007', 'bbbb0000-0000-0000-0000-00000000000f', 'COMPLETED', 140000, 
 '구매자1', '010-1000-0001', '서울시 송파구', '05551',
 'S-TRACK-006', 'B-TRACK-003', CURRENT_TIMESTAMP,
 'system', 'system', NULL);

-- 8. CANCELLED (주문 취소) - Buyer2 (ID: 3) -> Seller2 (ID: 6)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price, 
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    cancelled_at,
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000008', CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP, NULL, 
 3, 6, 'eeeeeeee-0000-0000-0000-000000000008', 'aaaa0000-0000-0000-0000-000000000001', 'CANCELLED', 150000, 
 '구매자2', '010-1000-0002', '인천시 연수구', '21999',
 CURRENT_TIMESTAMP,
 'system', 'system', NULL);
