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
