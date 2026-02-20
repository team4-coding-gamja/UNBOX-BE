# Resale A/B Test Report (Simulated) — 2026-02-18

## 0. Experiment Summary
- **Experiment ID**: `exp_cvr_home_v1`
- **Data Source**: 시뮬레이션(30,000 users / 14 days / 1,111,427 events)
- **실험 단위**: 사용자 단위 (user_id hash 50:50)
- **Primary Metric**: `match_to_checkout_pct` (buy-side, 보정 기준)
- **Secondary Metrics**: `checkout_to_payment_pct`, `match_to_abandon_pct`, `payment_failed_rate`
- **Note**: 시뮬레이션 결과이며 실데이터 기반 성과가 아님.

## 1. Problem & Hypothesis
- **문제정의**: 매칭 이후 결제 진입이 낮아 전환 손실이 발생한다는 가정.
- **가설**: 결제 진입 단계 개선이 전환율 개선에 가장 큰 영향을 준다.

## 2. Experiment Design
- **분할 방법**: user_id hash 50:50
- **대상 범위**: 전 카테고리 (리셀 도메인 시뮬레이션)
- **기간**: 14일 시뮬레이션

## 3. Metrics Definition (Resale)
### 3.1 구매자 퍼널
- `bid_to_match_pct` = match_bid / place_bid
- `match_to_checkout_pct` = checkout_start / match_bid
- `match_to_abandon_pct` = checkout_abandoned / match_bid
- `checkout_to_payment_pct` = payment_completed / checkout_start
- `checkout_to_fail_pct` = payment_failed / checkout_start

### 3.2 판매자 퍼널
- `ask_to_match_pct` = match_bid / set_ask
- `match_to_sell_pct` = sell_completed / match_bid

## 4. Results (Overall)
### 4.1 구매자 퍼널 (보정: trade_side = 'buy')
- `bid_to_match_pct`: **45.27%**
- `match_to_checkout_pct`: **58.22%**
- `match_to_abandon_pct`: **41.78%**

### 4.2 결제 단계
- `checkout_to_payment_pct`: **92.22%**
- `checkout_to_fail_pct`: **7.78%**

### 4.3 판매자 퍼널
- `ask_to_match_pct`: **89.70%**
- `match_to_sell_pct`: **74.03%**
- `list_to_sell_pct`: **66.40%**

## 5. Results (A/B)
### 5.1 이벤트 기준 CVR
| Metric | Control(A) | Variant(B) | Δ | p-value |
|---|---:|---:|---:|---:|
| view_to_payment (event CVR) | 4.44% | 4.45% | +0.01%p | N/A |

### 5.2 유저 기준 CVR
| Metric | Control(A) | Variant(B) | Δ | p-value |
|---|---:|---:|---:|---:|
| user_cvr | 44.67% | 45.31% | +0.64%p | N/A |

## 6. Results (Segmented)
### 6.1 Persona (구매자)
- `price_sensitive`: **79.58%**
- `impulse`: **49.53%**
- `browser`: **22.83%**

### 6.2 결제 실패 사유 분포
- `limit_exceeded`: 321
- `3ds_failed`: 310
- `card_declined`: 305
- `pg_error`: 289

### 6.3 매칭 후 이탈 사유 분포
- `verification_delay`: 3,803
- `payment_method_missing`: 3,752
- `time_limit`: 3,752

## 7. Interpretation
- 결제 실패는 **기술적 실패 수준(7.78%)**으로 제한됨.
- 핵심 병목은 **매칭 후 결제 진입 이탈(41.78%)** 구간으로 나타남.
- A/B 변이 간 차이는 미미 → 추가 실험 설계 필요.

## 8. Decision & Action
- **결론**: 결제 진입 단계 개선 가설을 우선 실험 대상으로 설정.
- **액션 아이템**:
  1. checkout_abandoned 원인별 메시지/리마인드 실험 설계
  2. 매칭 후 결제 UX 단축/원클릭 결제 실험 설계
  3. spread/latency 기반 매칭 품질 개선 실험

