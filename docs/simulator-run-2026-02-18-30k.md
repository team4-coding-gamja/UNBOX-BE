# Simulator Run Summary (2026-02-18, 30k Users, Resale Scenario)

## 목적
- 리셀 도메인(구매/판매 분리 플로우) 기반 이벤트 로그를 대규모로 생성하고
  퍼널/전환/매칭 지표 분석 가능성을 검증한다.

## 실행 환경
- 서비스: `unbox-ai` (FastAPI)
- 엔드포인트: `POST /api/v1/simulator/run`
- 적재 대상: Postgres `analytics.events`

## 입력 파라미터
```json
{
  "days": 14,
  "users": 30000,
  "avg_sessions_per_user": 1.2,
  "seller_ratio": 0.35,
  "seed": 45,
  "experiment_id": "exp_cvr_home_v1",
  "variant_splits": {"A": 0.5, "B": 0.5}
}
```

## 실행 결과 (요약)
- 사용자 수: 30,000
- 세션 수: 505,845
- 총 이벤트 수: 1,111,427

### 이벤트 분포
- `view_item`: 328,283
- `add_to_wishlist`: 77,291
- `place_bid`: 53,972
- `match_bid`: 156,565
- `checkout_start`: 6,013
- `payment_completed`: 1,248
- `payment_failed`: 4,765
- `bid_expired`: 10,391
- `list_item`: 177,562
- `set_ask`: 177,562
- `sell_completed`: 117,775

## 핵심 지표 (시뮬레이션 기준)
### 구매자 퍼널
- `view_item → add_to_wishlist`: **23.54%**
- `view_item → place_bid`: **16.44%**
- `view_item → checkout_start`: **1.83%**
- `view_item → payment_completed`: **0.38%**
- `checkout_start → payment_completed`: **20.76%**
- `checkout_start → payment_failed`: **79.24%**

### 판매자 퍼널
- `match_bid → sell_completed`: **75.22%**
- `list_item → sell_completed`: **66.33%**

## 해석 포인트
- 구매 플로우에서 **결제 구간이 가장 큰 병목**으로 나타남.
- 판매 플로우는 `match_bid` 이후 `sell_completed` 전환이 높은 편.
- 리셀 도메인의 특성(매칭 후 결제 이탈, 판매자 완료율 우위)이 데이터 상으로 재현됨.

## 확인 쿼리
```sql
SELECT event_name, COUNT(*)
FROM analytics.events
GROUP BY event_name
ORDER BY COUNT(*) DESC;
```

## 다음 단계 제안
1. A/B variant별 구매 전환율 비교
2. `spread`, `match_latency_sec` 기반 매칭 품질 분석
3. 결제 실패율 개선 가설 수립 및 실험 설계
