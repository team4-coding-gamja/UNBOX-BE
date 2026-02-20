# Simulator Run Summary (2026-02-18, 30k Users, Resale Scenario v2)

## 목적
- 리셀 도메인(구매/판매 분리) 시뮬레이터에서 **매칭 이후 이탈과 결제 실패를 현실적으로 분리**한 모델로 재실행 결과를 정리한다.

## 실행 결과 (쿼리 기반)
### 이벤트 분포
- `view_item`: 326,664
- `add_to_wishlist`: 83,070
- `place_bid`: 59,777
- `match_bid`: 157,665
- `checkout_start`: 15,755
- `checkout_abandoned`: 11,307
- `payment_completed`: 14,530
- `payment_failed`: 1,225
- `bid_expired`: 11,506
- `list_item`: 175,773
- `set_ask`: 175,773
- `sell_completed`: 116,717

## 구매자 퍼널 (이벤트 기준)
> 주의: `match_bid`는 판매/구매 모두 포함이므로, `bid_to_match_pct`는 직접 해석하지 않음.
- `view_item → add_to_wishlist`: **25.43%**
- `view_item → place_bid`: **18.30%**
- `match_bid → checkout_start`: **9.99%** (전체 match 기준)
- `match_bid → checkout_abandoned`: **7.17%** (전체 match 기준)
- `checkout_start → payment_completed`: **92.22%**
- `checkout_start → payment_failed`: **7.78%**
- `view_item → payment_completed` (전체 CVR): **4.45%**

## 판매자 퍼널
- `set_ask → match_bid`: **89.70%**
- `match_bid → sell_completed`: **74.03%**
- `list_item → sell_completed`: **66.40%**

## A/B 비교 (exp_cvr_home_v1)
### 이벤트 기준 CVR
- A: **4.44%** (7,222 / 162,532)
- B: **4.45%** (7,308 / 164,132)
→ 변이 간 차이 미미 (실질적 차이 없음)

### 유저 기준 CVR
- A: **44.67%** (4,327 / 9,686)
- B: **45.31%** (4,446 / 9,812)
→ 유저 기준에서도 유의미한 차이 없음

## 페르소나 전환율 (구매자)
- `price_sensitive`: **79.58%**
- `impulse`: **49.53%**
- `browser`: **22.83%**
> 판매자 페르소나는 `view_item` 기준이 아니라 `list_item` 기준 지표로 따로 봐야 함.

## 결제 실패/이탈 사유
### 결제 실패 (기술적 실패 가정)
- `limit_exceeded`: 321
- `3ds_failed`: 310
- `card_declined`: 305
- `pg_error`: 289

### 매칭 후 이탈
- `verification_delay`: 3,803
- `payment_method_missing`: 3,752
- `time_limit`: 3,752

## 해석 포인트
- **결제 성공률이 92% 이상**으로 개선되며, 매칭 이후 결제 실패가 “기술 실패” 범주로 축소됨.
- **매칭 후 이탈(`checkout_abandoned`)을 별도 이벤트로 분리**해 원인 분석 가능.
- **판매 플로우는 매칭 이후 완료율이 높고(74%)**, 구매 플로우는 매칭 이후 이탈/결제 실패 구간에 개선 여지가 있음.

## 보정 필요 사항
- `match_bid`는 판매/구매 모두 포함하므로, **구매자 퍼널 정확 분석 시 `trade_side = 'buy'` 필터 필요**.

### 보정 결과 (구매자 전용)
- `place_bid`: 59,777
- `match_bid`: 27,062
- `checkout_start`: 15,755
- `checkout_abandoned`: 11,307
- `payment_completed`: 14,530
- `bid_to_match_pct`: **45.27%**
- `match_to_checkout_pct`: **58.22%**
- `match_to_abandon_pct`: **41.78%**

### 보정 쿼리 (구매자 전용 match 기준)
```sql
WITH base AS (
  SELECT *
  FROM analytics.events
  WHERE properties->>'trade_side' = 'buy'
)
SELECT
  COUNT(*) FILTER (WHERE event_name = 'place_bid') AS place_bid,
  COUNT(*) FILTER (WHERE event_name = 'match_bid') AS match_bid,
  COUNT(*) FILTER (WHERE event_name = 'checkout_start') AS checkout_start,
  COUNT(*) FILTER (WHERE event_name = 'checkout_abandoned') AS checkout_abandoned,
  COUNT(*) FILTER (WHERE event_name = 'payment_completed') AS payment_completed,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'match_bid')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'place_bid'), 0) * 100,
    2
  ) AS bid_to_match_pct,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'checkout_start')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'match_bid'), 0) * 100,
    2
  ) AS match_to_checkout_pct,
  ROUND(
    COUNT(*) FILTER (WHERE event_name = 'checkout_abandoned')::numeric
    / NULLIF(COUNT(*) FILTER (WHERE event_name = 'match_bid'), 0) * 100,
    2
  ) AS match_to_abandon_pct
FROM base;
```
