-- =========================================
-- CLEANUP (Optional: for fresh load testing)
-- =========================================
DELETE FROM p_pg_transaction;
DELETE FROM p_payment;

-- 1) p_payment: READY 결제 100건
INSERT INTO p_payment (
    payment_id, order_id,
    buyer_id, seller_id,
    amount, method, status,
    payment_key,
    created_at, ready_at,
    selling_bid_id,
    version
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
    1 + (gs % 50)                 AS buyer_id,
    101 + (gs % 50)               AS seller_id,
    10000.00                      AS amount,
    'CARD'                         AS method,
    'READY'                        AS status,
    ('seed_ready_test_success_' || gs) AS payment_key,  -- ✅ 변경
    now()                          AS created_at,
    now()                          AS ready_at,
    (
        substr(md5('sell-' || gs::text), 1, 8) || '-' ||
        substr(md5('sell-' || gs::text), 9, 4) || '-' ||
        substr(md5('sell-' || gs::text), 13, 4) || '-' ||
        substr(md5('sell-' || gs::text), 17, 4) || '-' ||
        substr(md5('sell-' || gs::text), 21, 12)
        )::uuid AS selling_bid_id,
    0 AS version
FROM generate_series(1, 10000) gs
    ON CONFLICT (payment_id) DO UPDATE
                                    SET ready_at = now(),
                                    status = 'READY',
                                    deleted_at = NULL,
                                    version = 0;

-- 2) p_pg_transaction: READY 트랜잭션 10000건 (payment_id FK)
INSERT INTO p_pg_transaction (
    pg_transaction_id,
    payment_id, order_id,
    amount, currency, use_escrow,
    transaction_at, created_at,
    transaction_key, payment_key,
    method, status
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
    'KRW'        AS currency,
    false        AS use_escrow,
    now()        AS transaction_at,
    now()        AS created_at,
    ('tx_' || gs)              AS transaction_key,
    p.payment_key              AS payment_key,
    'CARD'                     AS method,
    'READY'                    AS status
FROM generate_series(1, 10000) gs
         JOIN p_payment p ON p.payment_key = ('seed_ready_test_success_' || gs)  -- ✅ 변경
    ON CONFLICT (pg_transaction_id) DO UPDATE
                                           SET status = 'READY';
