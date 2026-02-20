# Resale A/B Test Report (Simulated) — 2026-02-18 (Variant B: match_to_checkout +10%p)

## 0. Experiment Summary
- **Experiment ID**: `exp_cvr_home_v1`
- **Treatment**: Variant B에서 `match_to_checkout_p +10%p`
- **Data Source**: 시뮬레이션(30,000 users / 14 days)
- **실험 단위**: 사용자 단위 (user_id hash 50:50)
- **Primary Metric**: `match_to_checkout_pct` (buy-side)
- **Secondary Metrics**: `match_to_abandon_pct`, `checkout_to_payment_pct`, `view_to_payment` (event CVR)

## 1. Results (Primary)
### 1.1 Buy-side (trade_side = 'buy')
| Metric | Control(A) | Variant(B) | Δ |
|---|---:|---:|---:|
| match_to_checkout_pct | 57.63% | 68.45% | **+10.82%p** |
| match_to_abandon_pct | 42.37% | 31.55% | **-10.82%p** |

### 1.2 Counts
| Metric | A | B |
|---|---:|---:|
| match_bid | 13,716 | 13,774 |
| checkout_start | 7,905 | 9,428 |
| checkout_abandoned | 5,811 | 4,346 |

## 2. Results (Secondary)
### 2.1 Checkout → Payment
| Metric | Control(A) | Variant(B) | Δ |
|---|---:|---:|---:|
| checkout_to_payment_pct | 92.09% | 92.62% | +0.53%p |

### 2.2 Event CVR (전체)
| Metric | Control(A) | Variant(B) | Δ |
|---|---:|---:|---:|
| view_to_payment (event CVR) | 4.44% | 5.31% | **+0.87%p** |

## 3. Interpretation
- Variant B가 **매칭 후 결제 진입률을 +10%p 이상 개선**하며, 이탈률을 동일 폭으로 감소시킴.
- checkout_to_payment는 큰 변화 없음 → **개선 효과는 “결제 진입” 구간에 집중**됨.
- event CVR도 상승(4.44% → 5.31%) → 상위 퍼널 효과로 전이 가능성 확인.

## 4. Decision & Action
- **결론**: Variant B와 동일한 방향의 결제 진입 개선을 우선 실험 후보로 유지.
- **액션 아이템**:
  1. 결제 진입 UI/UX 단축 실험(원클릭, 간소화)
  2. 결제 진입 유도 메시지/리마인드 실험
  3. match_latency 및 spread 세그먼트별 효과 비교

## 5. Notes
- 시뮬레이션 결과이며 실데이터 기반의 유의미성 검정은 수행하지 않음.
