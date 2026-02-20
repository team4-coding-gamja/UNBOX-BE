-- =========================================
-- CLEANUP
-- =========================================
DELETE FROM p_orders;

-- 1) p_orders: PAYMENT_PENDING 100건 (결합을 위해 Payment seed와 동일한 ID 규칙)
INSERT INTO p_selling_bids (
    selling_id,
    created_at,
    brand_name,
    model_number,
    price,
    product_id,
    product_name,
    product_option_id,
    product_option_name,
    user_id,
    status,
    product_image_url,
    deadline,
    deleted_at -- ✅ 추가
)
SELECT
    (
        substr(md5('sell-' || gs::text), 1, 8) || '-' ||
        substr(md5('sell-' || gs::text), 9, 4) || '-' ||
        substr(md5('sell-' || gs::text), 13, 4) || '-' ||
        substr(md5('sell-' || gs::text), 17, 4) || '-' ||
        substr(md5('sell-' || gs::text), 21, 12)
        )::uuid AS selling_bids_id,
    now() AS created_at,
    'NIKE' AS brand_name,
    ('MODEL-' || gs) AS model_number,
    10000.00 AS price, -- 가격 일치 (10000원)
    (
        substr(md5('prod-' || gs::text), 1, 8) || '-' ||
        substr(md5('prod-' || gs::text), 9, 4) || '-' ||
        substr(md5('prod-' || gs::text), 13, 4) || '-' ||
        substr(md5('prod-' || gs::text), 17, 4) || '-' ||
        substr(md5('prod-' || gs::text), 21, 12)
        )::uuid AS product_id,
    ('AIR-MAX-' || gs) AS product_name,
    (
        substr(md5('opt-' || gs::text), 1, 8) || '-' ||
        substr(md5('opt-' || gs::text), 9, 4) || '-' ||
        substr(md5('opt-' || gs::text), 13, 4) || '-' ||
        substr(md5('opt-' || gs::text), 17, 4) || '-' ||
        substr(md5('opt-' || gs::text), 21, 12)
        )::uuid AS product_option_id,
    ('SIZE-' || (240 + (gs % 11) * 5)) AS product_option_name,
    101 + (gs % 50) AS user_id,
    'RESERVED' AS status,
    NULL AS product_image_url,
    NULL AS deadline,
    NULL AS deleted_at -- ✅ 명시적 NULL 설정
FROM generate_series(1, 10000) gs
    ON CONFLICT (selling_id) DO UPDATE SET status = 'RESERVED', deleted_at = NULL;
