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
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'user@unbox.com', '$2a$12$Fu11YxI5NCSBY8g6Nrr0rO/rzKCYIDN4ZbS64arRzr8xJLNpP8lQm', 'user1', '010-9999-8888', 'system', 'system', NULL),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'buyer1@unbox.com', '$2a$12$Fu11YxI5NCSBY8g6Nrr0rO/rzKCYIDN4ZbS64arRzr8xJLNpP8lQm', 'buyer1', '010-1000-0001', 'system', 'system', NULL),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'buyer2@unbox.com', '$2a$12$Fu11YxI5NCSBY8g6Nrr0rO/rzKCYIDN4ZbS64arRzr8xJLNpP8lQm', 'buyer2', '010-1000-0002', 'system', 'system', NULL),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'buyer3@unbox.com', '$2a$12$Fu11YxI5NCSBY8g6Nrr0rO/rzKCYIDN4ZbS64arRzr8xJLNpP8lQm', 'buyer3', '010-1000-0003', 'system', 'system', NULL),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'seller1@unbox.com', '$2a$12$Fu11YxI5NCSBY8g6Nrr0rO/rzKCYIDN4ZbS64arRzr8xJLNpP8lQm', 'seller1', '010-2000-0001', 'system', 'system', NULL),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'seller2@unbox.com', '$2a$12$Fu11YxI5NCSBY8g6Nrr0rO/rzKCYIDN4ZbS64arRzr8xJLNpP8lQm', 'seller2', '010-2000-0002', 'system', 'system', NULL),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'seller3@unbox.com', '$2a$12$Fu11YxI5NCSBY8g6Nrr0rO/rzKCYIDN4ZbS64arRzr8xJLNpP8lQm', 'seller3', '010-2000-0003', 'system', 'system', NULL);


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


-- =========================
-- AI Review Summary Test Data (Nike Dunk Low 리뷰 10개 추가)
-- Product ID: aaaaaaaa-0000-0000-0000-000000000001
-- Option ID: aaaa0000-0000-0000-0000-000000000001 (Size 240)
-- =========================

-- 1. Orders for Reviews (10개)
INSERT INTO p_orders (
    order_id, created_at, updated_at, deleted_at,
    buyer_id, seller_id, selling_bid_id, option_id, status, price,
    receiver_name, receiver_phone, receiver_address, receiver_zip_code,
    completed_at,
    created_by, updated_by, deleted_by
) VALUES
('cccccccc-0000-0000-0000-000000000010', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 2, 5, 'eeeeeeee-0000-0000-0000-000000000010', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer1', '010-0000-0010', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000011', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 3, 5, 'eeeeeeee-0000-0000-0000-000000000011', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer2', '010-0000-0011', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000012', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 4, 5, 'eeeeeeee-0000-0000-0000-000000000012', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer3', '010-0000-0012', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000013', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 2, 5, 'eeeeeeee-0000-0000-0000-000000000013', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer4', '010-0000-0013', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000014', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 3, 5, 'eeeeeeee-0000-0000-0000-000000000014', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer5', '010-0000-0014', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000015', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 4, 5, 'eeeeeeee-0000-0000-0000-000000000015', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer6', '010-0000-0015', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000016', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 2, 5, 'eeeeeeee-0000-0000-0000-000000000016', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer7', '010-0000-0016', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000017', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 3, 5, 'eeeeeeee-0000-0000-0000-000000000017', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer8', '010-0000-0017', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000018', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 4, 5, 'eeeeeeee-0000-0000-0000-000000000018', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer9', '010-0000-0018', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL),
('cccccccc-0000-0000-0000-000000000019', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 2, 5, 'eeeeeeee-0000-0000-0000-000000000019', 'aaaa0000-0000-0000-0000-000000000001', 'COMPLETED', 150000, 'Reviewer10', '010-0000-0019', 'Seoul', '00000', CURRENT_TIMESTAMP, 'system', 'system', NULL);

-- 2. Reviews (10개)
INSERT INTO p_review (
    review_id, created_at, updated_at, deleted_at,
    order_id, rating, content, image_url,
    created_by, updated_by, deleted_by
) VALUES
('dddddddd-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000010', 5, '정말 마음에 듭니다! 배송도 빠르고 사이즈도 딱 맞아요. 추천합니다.', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000011', 4, '전반적으로 만족하지만 생각보다 색감이 조금 다릅니다. 그래도 예뻐요.', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000012', 5, '최고의 신발입니다. 착화감이 예술이네요. 인생 신발 등극!', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000013', 3, '배송이 조금 늦어서 아쉬웠습니다. 상품 자체는 나쁘지 않아요.', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000014', 5, '선물용으로 샀는데 받는 사람이 너무 좋아해서 기분 좋네요. 포장도 꼼꼼했습니다.', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000006', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000015', 4, '정사이즈보다 약간 크게 나온 것 같아요. 반 사이즈 다운 추천합니다.', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000007', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000016', 5, '역시는 역시네요. 나이키 덩크는 실물이 깡패입니다. 너무 예뻐요.', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000008', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000017', 2, '박스가 찌그러져서 왔어요 ㅠㅠ 상품은 멀쩡해서 그냥 신습니다.', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000009', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000018', 5, '데일리로 신기 딱 좋습니다. 어디에나 잘 어울리는 디자인이에요.', NULL, 'system', 'system', NULL),
('dddddddd-0000-0000-0000-000000000010', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'cccccccc-0000-0000-0000-000000000019', 4, '가격 대비 훌륭합니다. 다음 딜 때 또 구매하고 싶네요.', NULL, 'system', 'system', NULL);
