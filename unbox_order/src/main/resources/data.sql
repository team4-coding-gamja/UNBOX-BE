-- =========================================
-- CLEANUP
-- =========================================
DELETE FROM p_orders;

-- 1) p_orders: PAYMENT_PENDING 100건 (결합을 위해 Payment seed와 동일한 ID 규칙)
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
    deleted_at -- ✅ 추가
)
SELECT
    -- order_id: Payment seed의 'ord-{gs}' 규칙과 동일
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

    -- payment_id: Payment seed의 'pay-{gs}' 규칙과 동일
    (
        substr(md5('pay-' || gs::text), 1, 8) || '-' ||
        substr(md5('pay-' || gs::text), 9, 4) || '-' ||
        substr(md5('pay-' || gs::text), 13, 4) || '-' ||
        substr(md5('pay-' || gs::text), 17, 4) || '-' ||
        substr(md5('pay-' || gs::text), 21, 12)
        )::uuid AS payment_id,

  -- selling_bid_id: Trade seed의 'sell-{gs}' 규칙과 동일
    (
        substr(md5('sell-' || gs::text), 1, 8) || '-' ||
        substr(md5('sell-' || gs::text), 9, 4) || '-' ||
        substr(md5('sell-' || gs::text), 13, 4) || '-' ||
        substr(md5('sell-' || gs::text), 17, 4) || '-' ||
        substr(md5('sell-' || gs::text), 21, 12)
        )::uuid AS selling_bid_id,

    NULL AS deleted_at -- ✅ 명시적 NULL 설정

FROM generate_series(1, 10000) gs
    ON CONFLICT (order_id) DO UPDATE SET status = 'PAYMENT_PENDING', deleted_at = NULL;
