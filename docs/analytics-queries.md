# Analytics SQL Query Set

## 사용 전 가이드
- 모든 쿼리는 `analytics.events`를 기준으로 작성됨.
- 기간 필터는 예시로 최근 7일 기준. 필요 시 수정.
- `experiment_id`, `variant_id`, `persona` 등은 이벤트의 공통/속성 필드 활용.

## 1. 퍼널 이벤트 카운트 (전체)
```sql
SELECT event_name, COUNT(*) AS cnt
FROM analytics.events
WHERE occurred_at >= now() - interval '7 days'
GROUP BY event_name
ORDER BY cnt DESC;
```

## 2. 퍼널 전환율 (이벤트 기준)
```sql
WITH counts AS (
  SELECT
    COUNT(*) FILTER (WHERE event_name = 'view_item') AS view_item,
    COUNT(*) FILTER (WHERE event_name = 'add_to_wishlist') AS add_to_wishlist,
    COUNT(*) FILTER (WHERE event_name = 'place_bid') AS place_bid,
    COUNT(*) FILTER (WHERE event_name = 'checkout_start') AS checkout_start,
    COUNT(*) FILTER (WHERE event_name = 'payment_completed') AS payment_completed
  FROM analytics.events
  WHERE occurred_at >= now() - interval '7 days'
)
SELECT
  view_item,
  add_to_wishlist,
  place_bid,
  checkout_start,
  payment_completed,
  ROUND(add_to_wishlist::numeric / NULLIF(view_item, 0) * 100, 2) AS view_to_wishlist_pct,
  ROUND(place_bid::numeric / NULLIF(view_item, 0) * 100, 2) AS view_to_bid_pct,
  ROUND(checkout_start::numeric / NULLIF(view_item, 0) * 100, 2) AS view_to_checkout_pct,
  ROUND(payment_completed::numeric / NULLIF(view_item, 0) * 100, 2) AS view_to_payment_pct,
  ROUND(payment_completed::numeric / NULLIF(checkout_start, 0) * 100, 2) AS checkout_to_payment_pct
FROM counts;
```

## 3. 유저 기준 퍼널 (Unique Users)
```sql
WITH user_events AS (
  SELECT user_id, event_name
  FROM analytics.events
  WHERE occurred_at >= now() - interval '7 days'
    AND user_id IS NOT NULL
)
SELECT
  event_name,
  COUNT(DISTINCT user_id) AS users
FROM user_events
GROUP BY event_name
ORDER BY users DESC;
```

## 4. 세션 기준 CVR
```sql
WITH sessions AS (
  SELECT
    session_id,
    BOOL_OR(event_name = 'view_item') AS has_view,
    BOOL_OR(event_name = 'payment_completed') AS has_payment
  FROM analytics.events
  WHERE occurred_at >= now() - interval '7 days'
    AND session_id IS NOT NULL
  GROUP BY session_id
)
SELECT
  COUNT(*) FILTER (WHERE has_view) AS sessions_with_view,
  COUNT(*) FILTER (WHERE has_payment) AS sessions_with_payment,
  ROUND(
    COUNT(*) FILTER (WHERE has_payment)::numeric
    / NULLIF(COUNT(*) FILTER (WHERE has_view), 0) * 100,
    2
  ) AS session_cvr_pct
FROM sessions;
```

## 5. A/B Variant 기준 비교 (이벤트 기준)
```sql
WITH base AS (
  SELECT *
  FROM analytics.events
  WHERE occurred_at >= now() - interval '7 days'
    AND experiment_id = 'exp_cvr_home_v1'
    AND variant_id IS NOT NULL
)
SELECT
  variant_id,
  COUNT(*) FILTER (WHERE event_name = 'view_item') AS view_item,
  COUNT(*) FILTER (WHERE event_name = 'checkout_start') AS checkout_start,
  COUNT(*) FILTER (WHERE event_name = 'payment_completed') AS payment_completed,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'payment_completed')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'view_item'), 0) * 100,
    2
  ) AS cvr_pct,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'payment_completed')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'checkout_start'), 0) * 100,
    2
  ) AS checkout_to_payment_pct
FROM base
GROUP BY variant_id
ORDER BY variant_id;
```

## 5-1. 매칭 이후 이탈/결제 전환 (리셀 특화)
```sql
WITH base AS (
  SELECT *
  FROM analytics.events
  WHERE occurred_at >= now() - interval '7 days'
)
SELECT
  COUNT(*) FILTER (WHERE event_name = 'match_bid') AS match_bid,
  COUNT(*) FILTER (WHERE event_name = 'checkout_start') AS checkout_start,
  COUNT(*) FILTER (WHERE event_name = 'checkout_abandoned') AS checkout_abandoned,
  COUNT(*) FILTER (WHERE event_name = 'payment_completed') AS payment_completed,
  COUNT(*) FILTER (WHERE event_name = 'payment_failed') AS payment_failed,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'checkout_start')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'match_bid'), 0) * 100,
    2
  ) AS match_to_checkout_pct,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'checkout_abandoned')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'match_bid'), 0) * 100,
    2
  ) AS match_to_abandon_pct,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'payment_completed')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'checkout_start'), 0) * 100,
    2
  ) AS checkout_to_payment_pct
FROM base;
```

## 5-2. 결제 실패 사유 분포
```sql
SELECT
  properties->>'failure_reason' AS failure_reason,
  COUNT(*) AS cnt
FROM analytics.events
WHERE event_name = 'payment_failed'
  AND occurred_at >= now() - interval '7 days'
GROUP BY properties->>'failure_reason'
ORDER BY cnt DESC;
```

## 5-3. Buy Now A/B (Round2 vs Round3 시뮬레이션)
```sql
WITH base AS (
  SELECT *
  FROM analytics.events
  WHERE experiment_id = 'exp_cvr_home_v1'
    AND properties->>'flow' = 'buy_now'
)
SELECT
  variant_id,
  COUNT(*) FILTER (WHERE event_name = 'buy_now_attempt') AS buy_now_attempt,
  COUNT(*) FILTER (WHERE event_name = 'lock_failed') AS lock_failed,
  COUNT(*) FILTER (WHERE event_name = 'next_best_offered') AS next_best_offered,
  COUNT(*) FILTER (WHERE event_name = 'next_best_accepted') AS next_best_accepted,
  COUNT(*) FILTER (WHERE event_name = 'checkout_start') AS checkout_start,
  COUNT(*) FILTER (WHERE event_name = 'payment_completed') AS payment_completed,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'payment_completed')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'buy_now_attempt'), 0) * 100,
    2
  ) AS buy_now_cvr_pct,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'next_best_accepted')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'next_best_offered'), 0) * 100,
    2
  ) AS next_best_accept_pct
FROM base
GROUP BY variant_id
ORDER BY variant_id;
```

## 5-4. Next Best 가격 상승폭 (Variant B)
```sql
SELECT
  AVG((properties->>'price_delta')::numeric) AS avg_price_delta,
  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY (properties->>'price_delta')::numeric) AS median_price_delta
FROM analytics.events
WHERE event_name = 'next_best_offered'
  AND properties->>'flow' = 'buy_now'
  AND experiment_id = 'exp_cvr_home_v1';
```
## 6. A/B Variant 기준 비교 (유저 기준 CVR)
```sql
WITH per_user AS (
  SELECT
    variant_id,
    user_id,
    BOOL_OR(event_name = 'view_item') AS viewed,
    BOOL_OR(event_name = 'payment_completed') AS paid
  FROM analytics.events
  WHERE occurred_at >= now() - interval '7 days'
    AND experiment_id = 'exp_cvr_home_v1'
    AND variant_id IS NOT NULL
    AND user_id IS NOT NULL
  GROUP BY variant_id, user_id
)
SELECT
  variant_id,
  COUNT(*) FILTER (WHERE viewed) AS users_viewed,
  COUNT(*) FILTER (WHERE paid) AS users_paid,
  ROUND(
    COUNT(*) FILTER (WHERE paid)::numeric
    / NULLIF(COUNT(*) FILTER (WHERE viewed), 0) * 100,
    2
  ) AS user_cvr_pct
FROM per_user
GROUP BY variant_id
ORDER BY variant_id;
```

## 7. Persona별 전환 비교 (properties JSON)
```sql
WITH per_user AS (
  SELECT
    properties->>'persona' AS persona,
    user_id,
    BOOL_OR(event_name = 'view_item') AS viewed,
    BOOL_OR(event_name = 'payment_completed') AS paid
  FROM analytics.events
  WHERE occurred_at >= now() - interval '7 days'
    AND user_id IS NOT NULL
    AND properties ? 'persona'
  GROUP BY properties->>'persona', user_id
)
SELECT
  persona,
  COUNT(*) FILTER (WHERE viewed) AS users_viewed,
  COUNT(*) FILTER (WHERE paid) AS users_paid,
  ROUND(
    COUNT(*) FILTER (WHERE paid)::numeric
    / NULLIF(COUNT(*) FILTER (WHERE viewed), 0) * 100,
    2
  ) AS persona_cvr_pct
FROM per_user
GROUP BY persona
ORDER BY persona_cvr_pct DESC;
```

## 8. Persona + Device 세그먼트 (심화)
```sql
WITH per_user AS (
  SELECT
    properties->>'persona' AS persona,
    properties->>'device' AS device,
    user_id,
    BOOL_OR(event_name = 'view_item') AS viewed,
    BOOL_OR(event_name = 'payment_completed') AS paid
  FROM analytics.events
  WHERE occurred_at >= now() - interval '7 days'
    AND user_id IS NOT NULL
    AND properties ? 'persona'
    AND properties ? 'device'
  GROUP BY properties->>'persona', properties->>'device', user_id
)
SELECT
  persona,
  device,
  COUNT(*) FILTER (WHERE viewed) AS users_viewed,
  COUNT(*) FILTER (WHERE paid) AS users_paid,
  ROUND(
    COUNT(*) FILTER (WHERE paid)::numeric
    / NULLIF(COUNT(*) FILTER (WHERE viewed), 0) * 100,
    2
  ) AS persona_device_cvr_pct
FROM per_user
GROUP BY persona, device
ORDER BY persona, device;
```
