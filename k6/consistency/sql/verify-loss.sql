-- Usage:
-- psql -U <user> -d unbox_payment -v run_id='20260213091500' -f k6/consistency/sql/verify-loss.sql

\set ON_ERROR_STOP on

\if :{?run_id}
\else
\echo '[ERROR] run_id is required. example: -v run_id=20260213091500'
\quit 1
\endif

\echo === Collect payment/outbox metrics (db: unbox_payment, run_id=:run_id) ===

SELECT COUNT(*)::int AS payment_done_count
FROM p_payment
WHERE status = 'DONE'
  AND payment_key LIKE ('test_success_' || :'run_id' || '_%')
\gset

SELECT COUNT(*)::int AS outbox_published_count
FROM payment_outbox
WHERE status = 'PUBLISHED'
  AND payload LIKE ('%test_success_' || :'run_id' || '_%')
\gset

SELECT COUNT(*)::int AS outbox_pending_count
FROM payment_outbox
WHERE status IN ('PENDING', 'PROCESSING')
  AND payload LIKE ('%test_success_' || :'run_id' || '_%')
\gset

\echo
\echo === Collect consumer metrics (db: unbox_order) ===
\connect unbox_order

SELECT CASE WHEN to_regclass('public.consumer_received_log') IS NULL THEN 0 ELSE 1 END AS consumer_log_exists
\gset

SELECT CASE
         WHEN to_regclass('public.consumer_received_log') IS NULL THEN -1
         ELSE COALESCE(
           (
             SELECT COUNT(DISTINCT payment_id)::int
             FROM consumer_received_log
             WHERE consumer_group = 'order-group'
               AND payment_key LIKE ('test_success_' || :'run_id' || '_%')
           ),
           0
         )
       END AS events_consumed_count
\gset

SELECT CASE
         WHEN to_regclass('public.consumer_received_log') IS NULL THEN -1
         ELSE COALESCE(
           (
             SELECT COUNT(*)::int
             FROM (
               SELECT payment_id
               FROM consumer_received_log
               WHERE consumer_group = 'order-group'
                 AND payment_key LIKE ('test_success_' || :'run_id' || '_%')
               GROUP BY payment_id
               HAVING COUNT(*) > 1
             ) t
           ),
           0
         )
       END AS duplicate_payment_id_count
\gset

\echo
\echo === Summary ===
SELECT
    :'run_id' AS run_id,
    :payment_done_count::int AS payment_done_count,
    :events_consumed_count::int AS events_consumed_count,
    :outbox_published_count::int AS outbox_published_count,
    :outbox_pending_count::int AS outbox_pending_count,
    :duplicate_payment_id_count::int AS duplicate_payment_id_count,
    GREATEST(
      :payment_done_count::int -
      CASE
        WHEN :events_consumed_count::int < 0 THEN 0
        ELSE :events_consumed_count::int
      END,
      0
    ) AS loss_count,
    CASE
      WHEN :payment_done_count::int = 0 THEN 0::numeric(10, 2)
      ELSE ROUND(
        (
          GREATEST(
            :payment_done_count::int -
            CASE
              WHEN :events_consumed_count::int < 0 THEN 0
              ELSE :events_consumed_count::int
            END,
            0
          )::numeric / :payment_done_count::numeric
        ) * 100,
        2
      )
    END AS loss_rate_percent,
    CASE
      WHEN :events_consumed_count::int < 0 THEN 'topic_observer_fallback'
      ELSE 'order_consumer_received_log'
    END AS consumed_source;

\if :consumer_log_exists
\echo
\echo === Duplicate Detail (consumer_received_log) ===
SELECT payment_id, COUNT(*) AS cnt
FROM consumer_received_log
WHERE consumer_group = 'order-group'
  AND payment_key LIKE ('test_success_' || :'run_id' || '_%')
GROUP BY payment_id
HAVING COUNT(*) > 1
ORDER BY cnt DESC, payment_id;
\else
\echo
\echo === consumer_received_log table not found: observer fallback only ===
\endif

\echo
\echo === Outbox Pending Detail (db: unbox_payment) ===
\connect unbox_payment
SELECT outbox_event_id, event_type, status, retry_count, created_at, published_at
FROM payment_outbox
WHERE payload LIKE ('%test_success_' || :'run_id' || '_%')
  AND status IN ('PENDING', 'PROCESSING', 'FAILED')
ORDER BY created_at ASC;
