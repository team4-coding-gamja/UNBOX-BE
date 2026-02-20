# Resale Simulator Summary (30k Users) — Slide-Style

## 1. 실행 개요
- **규모**: 30,000 users / 14 days / avg 1.2 sessions
- **모델**: 리셀 구매·판매 분리 + 매칭 후 이탈 분리
- **총 이벤트**: 1,111,427

## 2. 핵심 이벤트 분포
- `view_item`: 326,664
- `place_bid`: 59,777
- `match_bid`: 157,665
- `checkout_start`: 15,755
- `checkout_abandoned`: 11,307
- `payment_completed`: 14,530
- `payment_failed`: 1,225
- `sell_completed`: 116,717

## 3. 구매자 퍼널 (보정 전/후)
### 보정 전 (전체 match 기준)
- `match_bid → checkout_start`: **9.99%**
- `checkout_start → payment_completed`: **92.22%**
- `checkout_start → payment_failed`: **7.78%**
- `view_item → payment_completed`: **4.45%**

### 보정 후 (trade_side = buy)
- `bid_to_match_pct`: **45.27%**
- `match_to_checkout_pct`: **58.22%**
- `match_to_abandon_pct`: **41.78%**

## 4. 판매자 퍼널
- `set_ask → match_bid`: **89.70%**
- `match_bid → sell_completed`: **74.03%**
- `list_item → sell_completed`: **66.40%**

## 5. A/B 비교 (exp_cvr_home_v1)
### 이벤트 기준 CVR
- A: **4.44%**
- B: **4.45%**
→ 변이 차이 미미

### 유저 기준 CVR
- A: **44.67%**
- B: **45.31%**
→ 유의미한 차이 없음

## 6. 인사이트 요약
- **결제 실패율(7.78%)은 기술 실패 수준**으로 축소됨.
- **매칭 후 이탈(41.78%)이 핵심 병목**으로 확인됨.
- 판매 플로우는 매칭 이후 완료율이 높은 편(74%).

## 7. 다음 액션 제안
- 결제 전 단계(`checkout_abandoned`) 원인별 실험 설계
- 매칭 확률/스프레드 최적화 실험
- 구매자 전환 개선 A/B 테스트 설계
