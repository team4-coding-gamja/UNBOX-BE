-- =========================================
-- DEV 전용 주문 Seed (DELETE 없음)
-- - p_orders만 대상
-- - 기존 일반 데이터는 유지하고, 지정한 seed 범위만 upsert
-- 대상 DB: unbox_order (또는 주문 도메인 DB)
-- =========================================

-- [조정 포인트] payment seed와 동일한 범위를 사용해야 정합성이 맞습니다.
WITH params AS (
    SELECT 200001::int AS seed_start, 230000::int AS seed_end
), seed AS (
    SELECT gs
    FROM params, generate_series(seed_start, seed_end) gs
)
INSERT INTO p_orders (
    order_id,
    created_at,

    brand_name,
    buyer_id, buyer_name,

    model_number,
    price,

    product_id, product_name,
    product_option_id, product_option_name,

    receiver_address, receiver_name, receiver_phone, receiver_zip_code,

    seller_id,
    status,

    payment_id,
    selling_bid_id,
    deleted_at, deleted_by
)
SELECT
    (
        substr(md5('ord-' || gs::text), 1, 8) || '-' ||
        substr(md5('ord-' || gs::text), 9, 4) || '-' ||
        substr(md5('ord-' || gs::text), 13, 4) || '-' ||
        substr(md5('ord-' || gs::text), 17, 4) || '-' ||
        substr(md5('ord-' || gs::text), 21, 12)
    )::uuid AS order_id,

    now() AS created_at,

    'NIKE' AS brand_name,
    1 + (gs % 50) AS buyer_id,
    ('buyer-' || (1 + (gs % 50))) AS buyer_name,

    ('MODEL-' || gs) AS model_number,
    10000.00 AS price,

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

    ('Seoul Street ' || gs) AS receiver_address,
    ('receiver-' || gs) AS receiver_name,
    ('010-1234-' || lpad((gs % 10000)::text, 4, '0')) AS receiver_phone,
    ('ZIP-' || lpad((gs % 10000)::text, 4, '0')) AS receiver_zip_code,

    101 + (gs % 50) AS seller_id,
    'PAYMENT_PENDING' AS status,

    (
        substr(md5('pay-' || gs::text), 1, 8) || '-' ||
        substr(md5('pay-' || gs::text), 9, 4) || '-' ||
        substr(md5('pay-' || gs::text), 13, 4) || '-' ||
        substr(md5('pay-' || gs::text), 17, 4) || '-' ||
        substr(md5('pay-' || gs::text), 21, 12)
    )::uuid AS payment_id,

    (
        substr(md5('sell-' || gs::text), 1, 8) || '-' ||
        substr(md5('sell-' || gs::text), 9, 4) || '-' ||
        substr(md5('sell-' || gs::text), 13, 4) || '-' ||
        substr(md5('sell-' || gs::text), 17, 4) || '-' ||
        substr(md5('sell-' || gs::text), 21, 12)
    )::uuid AS selling_bid_id,

    NULL AS deleted_at,
    NULL AS deleted_by
FROM seed
ON CONFLICT (order_id) DO UPDATE
SET
    buyer_id = EXCLUDED.buyer_id,
    seller_id = EXCLUDED.seller_id,
    buyer_name = EXCLUDED.buyer_name,
    price = EXCLUDED.price,
    status = 'PAYMENT_PENDING',
    payment_id = EXCLUDED.payment_id,
    selling_bid_id = EXCLUDED.selling_bid_id,
    deleted_at = NULL,
    deleted_by = NULL;

-- 검증용 (해당 range 기준)
WITH params AS (
    SELECT 200001::int AS seed_start, 230000::int AS seed_end
), seed AS (
    SELECT gs
    FROM params, generate_series(seed_start, seed_end) gs
)
SELECT
    (SELECT COUNT(*) FROM seed) AS target_cnt,
    COUNT(*) AS order_payment_pending_cnt
FROM p_orders o
JOIN seed s ON o.payment_id = (
    (
        substr(md5('pay-' || s.gs::text), 1, 8) || '-' ||
        substr(md5('pay-' || s.gs::text), 9, 4) || '-' ||
        substr(md5('pay-' || s.gs::text), 13, 4) || '-' ||
        substr(md5('pay-' || s.gs::text), 17, 4) || '-' ||
        substr(md5('pay-' || s.gs::text), 21, 12)
    )::uuid
)
WHERE o.status = 'PAYMENT_PENDING';
