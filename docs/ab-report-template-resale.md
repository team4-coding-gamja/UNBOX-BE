# Resale A/B Test Report Template

## 0. Experiment Summary
- **Experiment ID**: `exp_...`
- **Owner / Team**: 
- **기간**: YYYY-MM-DD ~ YYYY-MM-DD
- **목표 지표 (Primary Metric)**: 예) `match_to_checkout_pct`, `checkout_to_payment_pct`
- **보조 지표 (Secondary Metrics)**: 예) `bid_to_match_pct`, `payment_failed_rate`
- **실험 대상**: 구매자 / 판매자 / 양쪽
- **변이(Variants)**: A (Control), B (Variant), ...

## 1. Problem & Hypothesis
- **문제정의**: 예) 매칭 이후 결제 진입률이 낮음
- **가설**: 예) 결제 프로세스 단축 시 매칭 후 이탈이 감소한다

## 2. Experiment Design
- **실험 단위**: 사용자 단위 / 세션 단위
- **분할 방법**: user_id hash 50:50
- **대상 범위**: 특정 상품군 / 전 카테고리
- **유의 수준**: 95% (필요 시 명시)
- **배제 조건**: 예) 테스트 계정, 내부 관리자 계정

## 3. Metrics Definition
### 3.1 구매자 퍼널
- `bid_to_match_pct` = match_bid / place_bid
- `match_to_checkout_pct` = checkout_start / match_bid
- `match_to_abandon_pct` = checkout_abandoned / match_bid
- `checkout_to_payment_pct` = payment_completed / checkout_start
- `checkout_to_fail_pct` = payment_failed / checkout_start

### 3.2 판매자 퍼널
- `ask_to_match_pct` = match_bid / set_ask
- `match_to_sell_pct` = sell_completed / match_bid

### 3.3 보조 지표
- 결제 실패 사유 분포
- 이탈 사유 분포
- match_latency_sec 평균/중앙값
- spread(ask-bid) 평균

## 4. Results (Overall)
| Metric | Control(A) | Variant(B) | Δ | p-value |
|---|---:|---:|---:|---:|
| match_to_checkout_pct |  |  |  |  |
| checkout_to_payment_pct |  |  |  |  |
| match_to_abandon_pct |  |  |  |  |
| bid_to_match_pct |  |  |  |  |
| payment_failed_rate |  |  |  |  |

## 5. Results (Segmented)
### 5.1 Persona
- `browser` vs `price_sensitive` vs `impulse`

### 5.2 Device
- `mobile` vs `desktop`

### 5.3 Category
- `sneakers`, `electronics`, ...

## 6. Interpretation
- 결과 해석 요약
- 기대 대비 변화량/영향 범위

## 7. Decision & Action
- **결론**: 롤아웃 / 보류 / 재실험
- **액션 아이템**:
  1. 
  2. 

## 8. Appendix (SQL Snippets)
```sql
-- variant별 구매자 퍼널
SELECT
  variant_id,
  COUNT(*) FILTER (WHERE event_name = 'match_bid') AS match_bid,
  COUNT(*) FILTER (WHERE event_name = 'checkout_start') AS checkout_start,
  COUNT(*) FILTER (WHERE event_name = 'checkout_abandoned') AS checkout_abandoned,
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
FROM analytics.events
WHERE experiment_id = 'exp_cvr_home_v1'
GROUP BY variant_id;
```
