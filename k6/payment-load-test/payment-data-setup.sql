-- =========================================
-- DEV 전용 결제 Seed (DELETE 없음)
-- - p_payment, p_pg_transaction만 대상
-- - 기존 일반 데이터는 유지하고, 지정한 seed 범위만 upsert
-- 대상 DB: unbox_payment (또는 결제 도메인 DB)
-- =========================================

-- [조정 포인트] 시드 범위
-- 다른 팀/테스트와 겹치지 않게 구간을 바꿔 사용하세요.
WITH params AS (
    SELECT 200001::int AS seed_start, 230000::int AS seed_end
), seed AS (
    SELECT gs
    FROM params, generate_series(seed_start, seed_end) gs
)
INSERT INTO p_payment (
    payment_id, order_id,
    buyer_id, seller_id,
    amount, method, status,
    payment_key,
    created_at, ready_at,
    selling_bid_id,
    version,
    deleted_at, deleted_by
)
SELECT
    (
        substr(md5('pay-' || gs::text), 1, 8) || '-' ||
        substr(md5('pay-' || gs::text), 9, 4) || '-' ||
        substr(md5('pay-' || gs::text), 13, 4) || '-' ||
        substr(md5('pay-' || gs::text), 17, 4) || '-' ||
        substr(md5('pay-' || gs::text), 21, 12)
    )::uuid AS payment_id,
    (
        substr(md5('ord-' || gs::text), 1, 8) || '-' ||
        substr(md5('ord-' || gs::text), 9, 4) || '-' ||
        substr(md5('ord-' || gs::text), 13, 4) || '-' ||
        substr(md5('ord-' || gs::text), 17, 4) || '-' ||
        substr(md5('ord-' || gs::text), 21, 12)
    )::uuid AS order_id,
    1 + (gs % 50) AS buyer_id,
    101 + (gs % 50) AS seller_id,
    10000.00 AS amount,
    'CARD' AS method,
    'READY' AS status,
    ('test_success_' || gs) AS payment_key,
    now() AS created_at,
    now() AS ready_at,
    (
        substr(md5('sell-' || gs::text), 1, 8) || '-' ||
        substr(md5('sell-' || gs::text), 9, 4) || '-' ||
        substr(md5('sell-' || gs::text), 13, 4) || '-' ||
        substr(md5('sell-' || gs::text), 17, 4) || '-' ||
        substr(md5('sell-' || gs::text), 21, 12)
    )::uuid AS selling_bid_id,
    0 AS version,
    NULL AS deleted_at,
    NULL AS deleted_by
FROM seed
ON CONFLICT (payment_id) DO UPDATE
SET
    order_id = EXCLUDED.order_id,
    buyer_id = EXCLUDED.buyer_id,
    seller_id = EXCLUDED.seller_id,
    amount = EXCLUDED.amount,
    method = EXCLUDED.method,
    status = 'READY',
    payment_key = EXCLUDED.payment_key,
    ready_at = now(),
    selling_bid_id = EXCLUDED.selling_bid_id,
    version = 0,
    deleted_at = NULL,
    deleted_by = NULL;

WITH params AS (
    SELECT 200001::int AS seed_start, 230000::int AS seed_end
), seed AS (
    SELECT gs
    FROM params, generate_series(seed_start, seed_end) gs
)
INSERT INTO p_pg_transaction (
    pg_transaction_id,
    payment_id, order_id,
    amount, currency, use_escrow,
    transaction_at, created_at,
    transaction_key, payment_key,
    method, status,
    deleted_at, deleted_by
)
SELECT
    (
        substr(md5('pg-' || gs::text), 1, 8) || '-' ||
        substr(md5('pg-' || gs::text), 9, 4) || '-' ||
        substr(md5('pg-' || gs::text), 13, 4) || '-' ||
        substr(md5('pg-' || gs::text), 17, 4) || '-' ||
        substr(md5('pg-' || gs::text), 21, 12)
    )::uuid AS pg_transaction_id,
    p.payment_id,
    p.order_id,
    p.amount,
    'KRW' AS currency,
    false AS use_escrow,
    now() AS transaction_at,
    now() AS created_at,
    ('tx_dev_' || gs) AS transaction_key,
    p.payment_key,
    'CARD' AS method,
    'READY' AS status,
    NULL AS deleted_at,
    NULL AS deleted_by
FROM seed
JOIN p_payment p ON p.payment_key = ('test_success_' || gs)
ON CONFLICT (pg_transaction_id) DO UPDATE
SET
    payment_id = EXCLUDED.payment_id,
    order_id = EXCLUDED.order_id,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    use_escrow = EXCLUDED.use_escrow,
    transaction_at = now(),
    transaction_key = EXCLUDED.transaction_key,
    payment_key = EXCLUDED.payment_key,
    method = EXCLUDED.method,
    status = 'READY',
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
    COUNT(*) AS payment_ready_cnt
FROM p_payment p
JOIN seed s ON p.payment_key = ('test_success_' || s.gs)
WHERE p.status = 'READY';
